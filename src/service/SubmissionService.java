package service;

import database.DatabaseConnection;
import model.Answer;
import model.Exam;
import model.Question;
import model.Submission;
import patterns.chainofresponsibility.AuthCheckHandler;
import patterns.chainofresponsibility.DuplicateSubmissionHandler;
import patterns.chainofresponsibility.ExamWindowHandler;
import patterns.chainofresponsibility.SubmissionContext;
import patterns.chainofresponsibility.SubmissionHandler;
import patterns.bridge.Notification;
import patterns.observer.ExamEvent;
import patterns.observer.ExamEventPublisher;
import repository.AnswerRepository;
import repository.ExamRepository;
import repository.QuestionRepository;
import repository.StudentRepository;
import repository.SubmissionRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** Core persistence workflow. Authentication and pattern-based checks follow in later milestones. */
public final class SubmissionService {
    private final DatabaseConnection database;
    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final StudentRepository students;
    private final SubmissionRepository submissions;
    private final AnswerRepository answers;
    private final Clock clock;
    private final SubmissionHandler validation;
    private final ExamEventPublisher events;
    private final Notification notifications;

    public SubmissionService(DatabaseConnection database, ExamRepository exams, QuestionRepository questions,
            StudentRepository students, SubmissionRepository submissions, AnswerRepository answers, Clock clock,
            ExamEventPublisher events, Notification notifications) {
        this.database = database;
        this.exams = exams;
        this.questions = questions;
        this.students = students;
        this.submissions = submissions;
        this.answers = answers;
        this.clock = clock;
        this.events = events;
        this.notifications = notifications;
        SubmissionHandler auth = new AuthCheckHandler(students);
        auth.then(new ExamWindowHandler()).then(new DuplicateSubmissionHandler(submissions));
        this.validation = auth;
    }

    public Submission submit(long examId, StudentSession student, Map<Long, String> responses) throws SQLException {
        if (responses == null) throw new IllegalArgumentException("Answers must not be null");
        Submission saved;
        ExamEvent event;
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                // Lock before checking for a previous submission. Competing calls for this
                // exam must wait and then see the first transaction's committed row.
                Exam exam = exams.lockById(connection, examId);
                LocalDateTime now = LocalDateTime.now(clock);
                validation.handle(new SubmissionContext(connection, exam, student, now));

                List<Question> examQuestions = questions.findByExamId(connection, examId);
                Map<Long, Question> permitted = new HashMap<>();
                for (Question question : examQuestions) permitted.put(question.getId(), question);
                for (Long questionId : responses.keySet()) {
                    if (questionId == null || !permitted.containsKey(questionId)) {
                        throw new IllegalArgumentException("Question does not belong to this exam");
                    }
                }

                saved = submissions.save(connection,
                        new Submission(null, examId, student.studentId(), now, "SUBMITTED", null));
                // Record even skipped questions so grading cannot ignore unanswered marks.
                for (Question question : examQuestions) {
                    answers.save(connection, new Answer(null, saved.getId(), question.getId(),
                            responses.getOrDefault(question.getId(), "")));
                }
                event = new ExamEvent(ExamEvent.Type.EXAM_SUBMITTED, examId, student.studentId(), now);
                events.publish(event, connection);
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    ex.addSuppressed(rollbackFailure);
                }
                throw ex;
            }
        }
        events.afterCommit(event);
        notifications.send("student-" + student.studentId(), "Submission #" + saved.getId() + " received");
        return saved;
    }

    /** Intended for teacher review; role authorization is not yet implemented. */
    public List<Submission> listForReview(TeacherSession teacher, long examId) throws SQLException {
        if (teacher == null) throw new IllegalArgumentException("Teacher login required");
        return submissions.findByExamId(examId);
    }

    public List<Answer> reviewAnswers(TeacherSession teacher, long submissionId) throws SQLException {
        if (teacher == null) throw new IllegalArgumentException("Teacher login required");
        try (Connection connection = database.open()) {
            return answers.findBySubmissionId(connection, submissionId);
        }
    }
}

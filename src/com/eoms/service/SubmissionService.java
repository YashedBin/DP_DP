package com.eoms.service;

import com.eoms.config.DatabaseConnection;
import com.eoms.model.Answer;
import com.eoms.model.Exam;
import com.eoms.model.Question;
import com.eoms.model.Submission;
import com.eoms.patterns.chainofresponsibility.AuthCheckHandler;
import com.eoms.patterns.chainofresponsibility.DuplicateSubmissionHandler;
import com.eoms.patterns.chainofresponsibility.ExamWindowHandler;
import com.eoms.patterns.chainofresponsibility.SubmissionContext;
import com.eoms.patterns.chainofresponsibility.SubmissionHandler;
import com.eoms.repository.AnswerRepository;
import com.eoms.repository.ExamRepository;
import com.eoms.repository.QuestionRepository;
import com.eoms.repository.StudentRepository;
import com.eoms.repository.SubmissionRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    public SubmissionService(DatabaseConnection database, ExamRepository exams, QuestionRepository questions,
            StudentRepository students, SubmissionRepository submissions, AnswerRepository answers, Clock clock) {
        this.database = database;
        this.exams = exams;
        this.questions = questions;
        this.students = students;
        this.submissions = submissions;
        this.answers = answers;
        this.clock = clock;
        SubmissionHandler auth = new AuthCheckHandler(students);
        auth.then(new ExamWindowHandler()).then(new DuplicateSubmissionHandler(submissions));
        this.validation = auth;
    }

    public Submission submit(long examId, StudentSession student, Map<Long, String> responses) throws SQLException {
        if (responses == null) throw new IllegalArgumentException("Answers must not be null");
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                // Lock before checking for a previous submission. Competing calls for this
                // exam must wait and then see the first transaction's committed row.
                Exam exam = exams.lockById(connection, examId);
                LocalDateTime now = LocalDateTime.now(clock);
                validation.handle(new SubmissionContext(connection, exam, student, now));

                for (Map.Entry<Long, String> response : responses.entrySet()) {
                    Long questionId = response.getKey();
                    if (questionId == null) {
                        throw new IllegalArgumentException("Invalid question ID");
                    }
                    Question question = questions.findById(connection, questionId);
                    if (question == null || question.getExamId() != examId) {
                        throw new IllegalArgumentException("Question does not belong to this exam");
                    }
                }

                Submission saved = submissions.save(connection,
                        new Submission(null, examId, student.studentId(), now, "SUBMITTED", null));
                for (Map.Entry<Long, String> response : responses.entrySet()) {
                    answers.save(connection, new Answer(null, saved.getId(), response.getKey(), response.getValue()));
                }
                connection.commit();
                return saved;
            } catch (SQLException | RuntimeException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    ex.addSuppressed(rollbackFailure);
                }
                throw ex;
            }
        }
    }

    /** Intended for teacher review; role authorization is not yet implemented. */
    public List<Submission> listForReview(long examId) throws SQLException {
        return submissions.findByExamId(examId);
    }
}

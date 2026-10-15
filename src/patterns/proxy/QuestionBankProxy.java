package patterns.proxy;

import database.DatabaseConnection;
import model.Exam;
import model.Question;
import repository.ExamRepository;
import repository.StudentRepository;
import repository.SubmissionRepository;
import service.StudentSession;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public final class QuestionBankProxy implements QuestionBank {
    private final DatabaseConnection database;
    private final ExamRepository exams;
    private final StudentRepository students;
    private final SubmissionRepository submissions;
    private final QuestionBank delegate;
    private final Clock clock;

    public QuestionBankProxy(DatabaseConnection database, ExamRepository exams, StudentRepository students,
            SubmissionRepository submissions, QuestionBank delegate, Clock clock) {
        this.database = database;
        this.exams = exams;
        this.students = students;
        this.submissions = submissions;
        this.delegate = delegate;
        this.clock = clock;
    }

    @Override
    public List<Question> forStudent(long examId, StudentSession student) throws SQLException {
        if (student == null) throw new IllegalArgumentException("Student login required");
        try (Connection connection = database.open()) {
            Exam exam = exams.findById(connection, examId);
            LocalDateTime now = LocalDateTime.now(clock);
            if (exam == null || !"ONGOING".equals(exam.getStatus()) ||
                    now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime())) {
                throw new IllegalStateException("Exam questions are not available");
            }
            if (students.findById(connection, student.studentId()) == null ||
                    submissions.exists(connection, examId, student.studentId())) {
                throw new IllegalStateException("Student is not eligible to view questions");
            }
        }
        return delegate.forStudent(examId, student);
    }
}

package com.eoms;

import com.eoms.config.ApplicationContext;
import com.eoms.config.DatabaseConnection;
import com.eoms.model.Exam;
import com.eoms.repository.ExamRepository;
import com.eoms.repository.QuestionRepository;
import com.eoms.repository.StudentRepository;
import com.eoms.service.DraftExamService;
import com.eoms.service.QuestionService;
import com.eoms.service.StudentService;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Run directly with java -ea; these checks never connect to a database or open Swing. */
public final class CoreDomainTest {
    public static void main(String[] args) throws SQLException {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC);
        DraftExamService drafts = new DraftExamService(clock);
        Exam exam = drafts.prepareDraft("  Midterm  ", 45, null);
        assert "Midterm".equals(exam.getTitle());
        assert "DRAFT".equals(exam.getStatus()) && exam.getId() == null;
        assert LocalDateTime.of(2026, 10, 15, 10, 45).equals(exam.getEndTime());
        expectInvalid(() -> drafts.prepareDraft(" ", 45, null));
        expectInvalid(() -> drafts.prepareDraft("Midterm", 0, null));

        DatabaseConnection missing = new DatabaseConnection(null, null, null);
        try {
            missing.open();
            throw new AssertionError("Missing credentials were accepted");
        } catch (SQLException expected) {
            assert expected.getMessage().contains("EOMS_DB_PASSWORD");
        }
        QuestionService questions = new QuestionService(new ExamRepository(missing), new QuestionRepository(missing));
        expectInvalid(() -> questions.add(1, "OTHER", "Question", 5, null));
        expectInvalid(() -> questions.add(1, "MCQ", "Question", 5, null));
        StudentService students = new StudentService(new StudentRepository(missing));
        expectInvalid(() -> students.register("", "student@example.test"));
        // Construction alone must not open the database or create a Swing window.
        new ApplicationContext();
        System.out.println("Core domain checks passed (database not contacted).");
    }

    private static void expectInvalid(SqlAction action) throws SQLException {
        try {
            action.run();
            throw new AssertionError("Invalid input was accepted");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private interface SqlAction {
        void run() throws SQLException;
    }
}

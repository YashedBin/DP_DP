package com.eoms.service;

import com.eoms.model.Exam;
import com.eoms.model.Question;
import com.eoms.config.DatabaseConnection;
import com.eoms.patterns.prototype.ExamPrototype;
import com.eoms.repository.ExamRepository;
import com.eoms.repository.QuestionRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class JdbcExamService implements ExamService {
    private final DraftExamService drafts;
    private final ExamRepository exams;
    private final Clock clock;
    private final DatabaseConnection database;
    private final QuestionRepository questions;

    public JdbcExamService(DraftExamService drafts, ExamRepository exams, QuestionRepository questions,
            DatabaseConnection database, Clock clock) {
        this.drafts = Objects.requireNonNull(drafts);
        this.exams = Objects.requireNonNull(exams);
        this.clock = Objects.requireNonNull(clock);
        this.database = Objects.requireNonNull(database);
        this.questions = Objects.requireNonNull(questions);
    }

    @Override
    public Exam prepareDraft(String title, int durationMinutes, LocalDateTime startTime) {
        return drafts.prepareDraft(title, durationMinutes, startTime);
    }

    @Override
    public Exam create(Exam draft) throws SQLException {
        if (draft == null || draft.getId() != null) {
            throw new IllegalArgumentException("Only new, unsaved exams can be created");
        }
        Exam valid = new com.eoms.patterns.builder.ExamBuilder(clock).title(draft.getTitle())
                .durationMinutes(draft.getDurationMinutes()).startTime(draft.getStartTime())
                .endTime(draft.getEndTime()).build();
        return exams.save(valid);
    }

    @Override
    public Exam findById(long id) throws SQLException {
        return exams.findById(id);
    }

    @Override
    public List<Exam> findAll() throws SQLException {
        return exams.findAll();
    }

    @Override
    public void start(long id) throws SQLException {
        Exam exam = exams.findById(id);
        if (exam == null || !"DRAFT".equals(exam.getStatus())) {
            throw new IllegalStateException("Only an existing draft exam can be started");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime())) {
            throw new IllegalStateException("Exam is outside its scheduled window");
        }
        if (!exams.transition(id, "DRAFT", "ONGOING")) {
            throw new IllegalStateException("Exam is no longer a draft");
        }
    }

    @Override
    public void end(long id) throws SQLException {
        if (!exams.transition(id, "ONGOING", "ENDED")) {
            throw new IllegalStateException("Only an ongoing exam can be ended");
        }
    }

    @Override
    public Exam duplicate(long id) throws SQLException {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                Exam source = exams.findById(connection, id);
                if (source == null) throw new IllegalArgumentException("Exam not found");
                ExamPrototype.Copy copy = ExamPrototype.duplicate(source, questions.findByExamId(connection, id));
                Exam newExam = exams.save(connection, copy.exam());
                for (Question question : copy.questions()) {
                    question.setExamId(newExam.getId());
                    questions.save(connection, question);
                }
                connection.commit();
                return newExam;
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException failure) { ex.addSuppressed(failure); }
                throw ex;
            }
        }
    }
}

package com.eoms.service;

import com.eoms.model.Exam;
import com.eoms.model.Question;
import com.eoms.config.DatabaseConnection;
import com.eoms.patterns.prototype.ExamPrototype;
import com.eoms.patterns.mediator.ExamLifecycleMediator;
import com.eoms.patterns.memento.ExamDraft;
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
    private final ExamLifecycleMediator lifecycle;

    public JdbcExamService(DraftExamService drafts, ExamRepository exams, QuestionRepository questions,
            DatabaseConnection database, Clock clock, ExamLifecycleMediator lifecycle) {
        this.drafts = Objects.requireNonNull(drafts);
        this.exams = Objects.requireNonNull(exams);
        this.clock = Objects.requireNonNull(clock);
        this.database = Objects.requireNonNull(database);
        this.questions = Objects.requireNonNull(questions);
        this.lifecycle = Objects.requireNonNull(lifecycle);
    }

    @Override
    public Exam prepareDraft(String title, int durationMinutes, LocalDateTime startTime) {
        return drafts.prepareDraft(title, durationMinutes, startTime);
    }

    @Override
    public Exam create(TeacherSession teacher, Exam draft) throws SQLException {
        requireTeacher(teacher);
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
    public void start(TeacherSession teacher, long id) throws SQLException {
        requireTeacher(teacher);
        lifecycle.start(id);
    }

    @Override
    public void end(TeacherSession teacher, long id) throws SQLException {
        requireTeacher(teacher);
        lifecycle.end(id);
    }

    @Override
    public Exam duplicate(TeacherSession teacher, long id) throws SQLException {
        requireTeacher(teacher);
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                Exam source = exams.findById(connection, id);
                if (source == null) throw new IllegalArgumentException("Exam not found");
                List<Question> originals = questions.findByExamId(connection, id);
                ExamPrototype.Copy copy = ExamPrototype.duplicate(source, originals);
                Exam newExam = exams.save(connection, copy.exam());
                for (int index = 0; index < copy.questions().size(); index++) {
                    Question question = copy.questions().get(index);
                    question.setExamId(newExam.getId());
                    questions.save(connection, question);
                    questions.copyKey(connection, originals.get(index).getId(), question.getId());
                }
                connection.commit();
                return newExam;
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException failure) { ex.addSuppressed(failure); }
                throw ex;
            }
        }
    }

    @Override
    public void restoreDraft(TeacherSession teacher, long id, ExamDraft draft) throws SQLException {
        requireTeacher(teacher);
        if (draft == null) throw new IllegalArgumentException("Draft required");
        Exam original = draft.exam();
        Exam valid = new com.eoms.patterns.builder.ExamBuilder(clock).title(original.getTitle())
                .durationMinutes(original.getDurationMinutes()).startTime(original.getStartTime())
                .endTime(original.getEndTime()).build();
        ExamPrototype.Copy detached = ExamPrototype.duplicate(valid, draft.questions());
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                Exam current = exams.lockById(connection, id);
                if (current == null || !"DRAFT".equals(current.getStatus())) {
                    throw new IllegalStateException("Only draft exams can be restored");
                }
                exams.updateDraft(connection, id, valid);
                questions.deleteForDraft(connection, id);
                List<String> keys = draft.answerKeys();
                for (int index = 0; index < detached.questions().size(); index++) {
                    Question question = detached.questions().get(index);
                    question.setExamId(id);
                    questions.save(connection, question);
                    questions.saveKey(connection, question.getId(), keys.get(index));
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException failure) { ex.addSuppressed(failure); }
                throw ex;
            }
        }
    }

    @Override
    public void configureDraft(TeacherSession teacher, long id, Exam draft) throws SQLException {
        requireTeacher(teacher);
        if (draft == null) throw new IllegalArgumentException("Draft required");
        Exam valid = new com.eoms.patterns.builder.ExamBuilder(clock).title(draft.getTitle())
                .durationMinutes(draft.getDurationMinutes()).startTime(draft.getStartTime())
                .endTime(draft.getEndTime()).build();
        try (Connection connection = database.open()) {
            exams.updateDraft(connection, id, valid);
        }
    }

    private static void requireTeacher(TeacherSession teacher) {
        if (teacher == null) throw new IllegalArgumentException("Teacher login required");
    }
}

package com.eoms.service;

import com.eoms.model.Exam;
import com.eoms.repository.ExamRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class JdbcExamService implements ExamService {
    private final DraftExamService drafts;
    private final ExamRepository exams;
    private final Clock clock;

    public JdbcExamService(DraftExamService drafts, ExamRepository exams, Clock clock) {
        this.drafts = Objects.requireNonNull(drafts);
        this.exams = Objects.requireNonNull(exams);
        this.clock = Objects.requireNonNull(clock);
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
        Exam valid = drafts.prepareDraft(draft.getTitle(), draft.getDurationMinutes(), draft.getStartTime());
        if (draft.getEndTime() != null && !draft.getEndTime().isAfter(valid.getStartTime())) {
            throw new IllegalArgumentException("Exam end time must follow its start time");
        }
        if (draft.getEndTime() != null) valid.setEndTime(draft.getEndTime());
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
}

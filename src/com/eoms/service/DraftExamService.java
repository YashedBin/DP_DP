package com.eoms.service;

import com.eoms.model.Exam;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public final class DraftExamService {
    private final Clock clock;

    public DraftExamService(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public Exam prepareDraft(String title, int durationMinutes, LocalDateTime startTime) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Exam title is required");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        LocalDateTime start = startTime == null ? LocalDateTime.now(clock) : startTime;
        return new Exam(null, title.trim(), durationMinutes, start,
                start.plusMinutes(durationMinutes), "DRAFT");
    }

}

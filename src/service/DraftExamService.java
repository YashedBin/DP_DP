package service;

import model.Exam;
import patterns.builder.ExamBuilder;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public final class DraftExamService {
    private final Clock clock;

    public DraftExamService(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public Exam prepareDraft(String title, int durationMinutes, LocalDateTime startTime) {
        return new ExamBuilder(clock).title(title).durationMinutes(durationMinutes)
                .startTime(startTime).build();
    }

}

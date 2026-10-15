package patterns.builder;

import model.Exam;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/** Builds a new unsaved exam; persisted identities are never supplied here. */
public final class ExamBuilder {
    private final Clock clock;
    private String title;
    private int durationMinutes = 60;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public ExamBuilder(Clock clock) { this.clock = Objects.requireNonNull(clock); }
    public ExamBuilder title(String value) { title = value; return this; }
    public ExamBuilder durationMinutes(int value) { durationMinutes = value; return this; }
    public ExamBuilder startTime(LocalDateTime value) { startTime = value; return this; }
    public ExamBuilder endTime(LocalDateTime value) { endTime = value; return this; }

    public Exam build() {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Exam title is required");
        if (durationMinutes <= 0) throw new IllegalArgumentException("Duration must be positive");
        LocalDateTime start = startTime == null ? LocalDateTime.now(clock) : startTime;
        LocalDateTime end = endTime == null ? start.plusMinutes(durationMinutes) : endTime;
        if (!end.isAfter(start)) throw new IllegalArgumentException("End time must follow start time");
        return new Exam(null, title.trim(), durationMinutes, start, end, "DRAFT");
    }
}

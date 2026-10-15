package patterns.observer;

import java.time.LocalDateTime;

public record ExamEvent(Type type, long examId, Long studentId, LocalDateTime occurredAt) {
    public enum Type { EXAM_STARTED, EXAM_ENDED, EXAM_SUBMITTED, EXAM_PUBLISHED }
}

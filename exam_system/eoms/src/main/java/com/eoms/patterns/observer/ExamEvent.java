package com.eoms.patterns.observer;

import java.time.LocalDateTime;

public class ExamEvent {

    public enum Type { EXAM_STARTED, EXAM_SUBMITTED, EXAM_GRADED, EXAM_PUBLISHED }

    private final Type type;
    private final Long examId;
    private final Long studentId;
    private final LocalDateTime occurredAt;
    private final String details;

    public ExamEvent(Type type, Long examId, Long studentId, LocalDateTime occurredAt, String details) {
        this.type = type;
        this.examId = examId;
        this.studentId = studentId;
        this.occurredAt = occurredAt;
        this.details = details;
    }

    public Type getType() { return type; }
    public Long getExamId() { return examId; }
    public Long getStudentId() { return studentId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getDetails() { return details; }
}

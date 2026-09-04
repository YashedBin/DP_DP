package com.eoms.patterns.chainofresponsibility;

import com.eoms.entity.Exam;

public class SubmissionContext {

    private final Exam exam;
    private final Long studentId;
    private boolean rejected = false;
    private String rejectionReason;

    public SubmissionContext(Exam exam, Long studentId) {
        this.exam = exam;
        this.studentId = studentId;
    }

    public Exam getExam() { return exam; }
    public Long getStudentId() { return studentId; }

    public boolean isRejected() { return rejected; }

    public String getRejectionReason() { return rejectionReason; }

    public void reject(String reason) {
        this.rejected = true;
        this.rejectionReason = reason;
    }
}

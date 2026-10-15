package com.eoms.patterns.chainofresponsibility;

public final class ExamWindowHandler extends SubmissionHandler {
    @Override
    protected void check(SubmissionContext context) {
        if (context.exam() == null || !"ONGOING".equals(context.exam().getStatus())) {
            throw new IllegalStateException("Exam is not ongoing");
        }
        if (context.now().isBefore(context.exam().getStartTime()) ||
                context.now().isAfter(context.exam().getEndTime())) {
            throw new IllegalStateException("Exam is outside its scheduled window");
        }
    }
}

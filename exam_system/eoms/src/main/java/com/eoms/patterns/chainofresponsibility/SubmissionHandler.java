package com.eoms.patterns.chainofresponsibility;

public abstract class SubmissionHandler {

    private SubmissionHandler next;

    public SubmissionHandler setNext(SubmissionHandler next) {
        this.next = next;
        return next;
    }

    public void handle(SubmissionContext context) {
        check(context);
        if (!context.isRejected() && next != null) {
            next.handle(context);
        }
    }

    protected abstract void check(SubmissionContext context);
}

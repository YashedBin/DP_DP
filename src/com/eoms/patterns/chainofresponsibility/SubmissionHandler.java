package com.eoms.patterns.chainofresponsibility;

import java.sql.SQLException;

public abstract class SubmissionHandler {
    private SubmissionHandler next;

    public SubmissionHandler then(SubmissionHandler successor) {
        next = successor;
        return successor;
    }

    public final void handle(SubmissionContext context) throws SQLException {
        check(context);
        if (next != null) next.handle(context);
    }

    protected abstract void check(SubmissionContext context) throws SQLException;
}

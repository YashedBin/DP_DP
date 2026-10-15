package patterns.chainofresponsibility;

import repository.SubmissionRepository;
import java.sql.SQLException;

public final class DuplicateSubmissionHandler extends SubmissionHandler {
    private final SubmissionRepository submissions;

    public DuplicateSubmissionHandler(SubmissionRepository submissions) { this.submissions = submissions; }

    @Override
    protected void check(SubmissionContext context) throws SQLException {
        if (submissions.exists(context.connection(), context.exam().getId(), context.student().studentId())) {
            throw new IllegalStateException("Student has already submitted this exam");
        }
    }
}

package com.eoms.patterns.chainofresponsibility;

import com.eoms.repository.StudentRepository;
import java.sql.SQLException;

public final class AuthCheckHandler extends SubmissionHandler {
    private final StudentRepository students;

    public AuthCheckHandler(StudentRepository students) { this.students = students; }

    @Override
    protected void check(SubmissionContext context) throws SQLException {
        if (context.student() == null || context.student().studentId() <= 0) {
            throw new IllegalArgumentException("Student login required");
        }
        if (students.findById(context.connection(), context.student().studentId()) == null) {
            throw new IllegalArgumentException("Student account no longer exists");
        }
    }
}

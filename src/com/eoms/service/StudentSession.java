package com.eoms.service;

/** In-process identity returned only after checking a student's stored password. */
public final class StudentSession {
    private final long studentId;
    StudentSession(long studentId) { this.studentId = studentId; }
    public long studentId() { return studentId; }
}

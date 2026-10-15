package service;

/** In-process teacher identity returned after password verification. */
public final class TeacherSession {
    private final long teacherId;
    TeacherSession(long teacherId) { this.teacherId = teacherId; }
    public long teacherId() { return teacherId; }
}

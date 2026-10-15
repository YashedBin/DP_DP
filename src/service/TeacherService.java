package service;

import repository.TeacherRepository;
import java.sql.SQLException;

public final class TeacherService {
    private final TeacherRepository teachers;
    public TeacherService(TeacherRepository teachers) { this.teachers = teachers; }

    public TeacherSession login(String username, String password) throws SQLException {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username required");
        TeacherRepository.Credentials credentials = teachers.credentials(username.trim());
        if (credentials == null || !PasswordHashes.matches(password, credentials.passwordHash())) {
            throw new IllegalArgumentException("Invalid teacher credentials");
        }
        return new TeacherSession(credentials.id());
    }
}

package repository;

import database.DatabaseConnection;
import model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class StudentRepository {
    private final DatabaseConnection database;

    public StudentRepository(DatabaseConnection database) {
        this.database = database;
    }

    public Student findById(long id) throws SQLException {
        try (Connection connection = database.open()) {
            return findById(connection, id);
        }
    }

    public Student findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, email FROM students WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? new Student(rows.getLong("id"), rows.getString("name"),
                        rows.getString("email")) : null;
            }
        }
    }

    public Student save(Student student, String passwordHash) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO students (name, email, password_hash) VALUES (?, ?, ?) RETURNING id")) {
            statement.setString(1, student.getName());
            statement.setString(2, student.getEmail());
            statement.setString(3, passwordHash);
            try (ResultSet keys = statement.executeQuery()) {
                if (!keys.next()) throw new SQLException("Student insert returned no ID");
                student.setId(keys.getLong(1));
                return student;
            }
        }
    }

    public String passwordHash(long studentId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT password_hash FROM students WHERE id = ?")) {
            statement.setLong(1, studentId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString(1) : null;
            }
        }
    }

    public void setPassword(long id, String hash) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE students SET password_hash = ? WHERE id = ?")) {
            statement.setString(1, hash);
            statement.setLong(2, id);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Student not found");
        }
    }
}

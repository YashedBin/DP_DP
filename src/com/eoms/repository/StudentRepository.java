package com.eoms.repository;

import com.eoms.config.DatabaseConnection;
import com.eoms.model.Student;
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

    public Student save(Student student) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO students (name, email) VALUES (?, ?) RETURNING id")) {
            statement.setString(1, student.getName());
            statement.setString(2, student.getEmail());
            try (ResultSet keys = statement.executeQuery()) {
                if (!keys.next()) throw new SQLException("Student insert returned no ID");
                student.setId(keys.getLong(1));
                return student;
            }
        }
    }
}

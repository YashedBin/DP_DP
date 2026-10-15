package com.eoms.repository;

import com.eoms.config.DatabaseConnection;
import com.eoms.model.Exam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class ExamRepository {
    private final DatabaseConnection database;

    public ExamRepository(DatabaseConnection database) {
        this.database = database;
    }

    public List<Exam> findAll() throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, title, duration_minutes, start_time, end_time, status FROM exams ORDER BY id");
                ResultSet rows = statement.executeQuery()) {
            List<Exam> exams = new ArrayList<>();
            while (rows.next()) exams.add(read(rows));
            return exams;
        }
    }

    public Exam findById(long id) throws SQLException {
        try (Connection connection = database.open()) {
            return findById(connection, id);
        }
    }

    public Exam findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, title, duration_minutes, start_time, end_time, status FROM exams WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        }
    }

    /** Locks the exam row so concurrent submissions can serialize duplicate checks. */
    public Exam lockById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, title, duration_minutes, start_time, end_time, status FROM exams WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        }
    }

    public Exam save(Exam exam) throws SQLException {
        try (Connection connection = database.open()) {
            return save(connection, exam);
        }
    }

    public Exam save(Connection connection, Exam exam) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO exams (title, duration_minutes, start_time, end_time, status) "
                                + "VALUES (?, ?, ?, ?, ?) RETURNING id")) {
            statement.setString(1, exam.getTitle());
            statement.setInt(2, exam.getDurationMinutes());
            statement.setTimestamp(3, Timestamp.valueOf(exam.getStartTime()));
            statement.setTimestamp(4, Timestamp.valueOf(exam.getEndTime()));
            statement.setString(5, exam.getStatus());
            try (ResultSet keys = statement.executeQuery()) {
                if (!keys.next()) throw new SQLException("Exam insert returned no ID");
                exam.setId(keys.getLong(1));
                return exam;
            }
        }
    }

    public void updateDraft(Connection connection, long id, Exam draft) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE exams SET title = ?, duration_minutes = ?, start_time = ?, end_time = ? "
                        + "WHERE id = ? AND status = 'DRAFT'")) {
            statement.setString(1, draft.getTitle());
            statement.setInt(2, draft.getDurationMinutes());
            statement.setTimestamp(3, Timestamp.valueOf(draft.getStartTime()));
            statement.setTimestamp(4, Timestamp.valueOf(draft.getEndTime()));
            statement.setLong(5, id);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("Exam is no longer a draft");
        }
    }

    public boolean transition(Connection connection, long id, String from, String to) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE exams SET status = ? WHERE id = ? AND status = ?")) {
            statement.setString(1, to);
            statement.setLong(2, id);
            statement.setString(3, from);
            return statement.executeUpdate() == 1;
        }
    }

    private static Exam read(ResultSet rows) throws SQLException {
        return new Exam(rows.getLong("id"), rows.getString("title"), rows.getInt("duration_minutes"),
                rows.getTimestamp("start_time").toLocalDateTime(),
                rows.getTimestamp("end_time").toLocalDateTime(), rows.getString("status"));
    }
}

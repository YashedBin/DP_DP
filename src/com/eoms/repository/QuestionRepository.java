package com.eoms.repository;

import com.eoms.config.DatabaseConnection;
import com.eoms.model.Question;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Internal data access; callers must enforce question visibility in services. */
public final class QuestionRepository {
    private final DatabaseConnection database;

    public QuestionRepository(DatabaseConnection database) {
        this.database = database;
    }

    public List<Question> findByExamId(long examId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, exam_id, type, text, marks, options FROM questions WHERE exam_id = ? ORDER BY id")) {
            statement.setLong(1, examId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Question> questions = new ArrayList<>();
                while (rows.next()) questions.add(read(rows));
                return questions;
            }
        }
    }

    public Question findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, exam_id, type, text, marks, options FROM questions WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        }
    }

    public Question save(Question question) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO questions (exam_id, type, text, marks, options) "
                                + "VALUES (?, ?, ?, ?, ?) RETURNING id")) {
            statement.setLong(1, question.getExamId());
            statement.setString(2, question.getType());
            statement.setString(3, question.getText());
            statement.setInt(4, question.getMarks());
            statement.setString(5, question.getOptions());
            try (ResultSet keys = statement.executeQuery()) {
                if (!keys.next()) throw new SQLException("Question insert returned no ID");
                question.setId(keys.getLong(1));
                return question;
            }
        }
    }

    public boolean update(Question question) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE questions SET type = ?, text = ?, marks = ?, options = ? WHERE id = ? AND exam_id = ?")) {
            statement.setString(1, question.getType());
            statement.setString(2, question.getText());
            statement.setInt(3, question.getMarks());
            statement.setString(4, question.getOptions());
            statement.setLong(5, question.getId());
            statement.setLong(6, question.getExamId());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(long examId, long id) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM questions WHERE id = ? AND exam_id = ?")) {
            statement.setLong(1, id);
            statement.setLong(2, examId);
            return statement.executeUpdate() == 1;
        }
    }

    private static Question read(ResultSet rows) throws SQLException {
        return new Question(rows.getLong("id"), rows.getLong("exam_id"), rows.getString("type"),
                rows.getString("text"), rows.getInt("marks"), rows.getString("options"));
    }
}

package repository;

import database.DatabaseConnection;
import model.Question;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** Internal data access; callers must enforce question visibility in services. */
public final class QuestionRepository {
    private final DatabaseConnection database;

    public QuestionRepository(DatabaseConnection database) {
        this.database = database;
    }

    public List<Question> findByExamId(long examId) throws SQLException {
        try (Connection connection = database.open()) {
            return findByExamId(connection, examId);
        }
    }

    public List<Question> findByExamId(Connection connection, long examId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, exam_id, type, text, marks, options FROM questions WHERE exam_id = ? ORDER BY id")) {
            statement.setLong(1, examId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Question> questions = new ArrayList<>();
                while (rows.next()) questions.add(read(rows));
                return questions;
            }
        }
    }

    public Question save(Question question) throws SQLException {
        try (Connection connection = database.open()) {
            return save(connection, question);
        }
    }

    public Question save(Connection connection, Question question) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
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

    public void deleteForDraft(Connection connection, long examId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM questions WHERE exam_id = ?")) {
            statement.setLong(1, examId);
            statement.executeUpdate();
        }
    }

    /** Transfer MCQ answer key when copying a question; never reads it through student access. */
    public void copyKey(Connection connection, long sourceId, long destinationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO question_keys (question_id, correct_answer) "
                        + "SELECT ?, correct_answer FROM question_keys WHERE question_id = ?")) {
            statement.setLong(1, destinationId);
            statement.setLong(2, sourceId);
            statement.executeUpdate();
        }
    }

    /** Teacher-only draft content. Do not call this from student question access. */
    public Map<Long, String> keysForExam(long examId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT k.question_id, k.correct_answer FROM question_keys k "
                        + "JOIN questions q ON q.id = k.question_id "
                        + "JOIN exams e ON e.id = q.exam_id WHERE e.id = ? AND e.status = 'DRAFT'")) {
            statement.setLong(1, examId);
            try (ResultSet rows = statement.executeQuery()) {
                Map<Long, String> result = new HashMap<>();
                while (rows.next()) result.put(rows.getLong(1), rows.getString(2));
                return result;
            }
        }
    }

    public void saveKey(Connection connection, long questionId, String answer) throws SQLException {
        if (answer == null) return;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO question_keys (question_id, correct_answer) VALUES (?, ?)")) {
            statement.setLong(1, questionId);
            statement.setString(2, answer);
            statement.executeUpdate();
        }
    }

    private static Question read(ResultSet rows) throws SQLException {
        return new Question(rows.getLong("id"), rows.getLong("exam_id"), rows.getString("type"),
                rows.getString("text"), rows.getInt("marks"), rows.getString("options"));
    }
}

package repository;

import database.DatabaseConnection;
import model.Submission;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class SubmissionRepository {
    private final DatabaseConnection database;

    public SubmissionRepository(DatabaseConnection database) {
        this.database = database;
    }

    public boolean exists(Connection connection, long examId, long studentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM submissions WHERE exam_id = ? AND student_id = ? LIMIT 1")) {
            statement.setLong(1, examId);
            statement.setLong(2, studentId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    public List<Submission> findByExamId(long examId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, exam_id, student_id, submitted_at, status, score "
                                + "FROM submissions WHERE exam_id = ? ORDER BY id")) {
            statement.setLong(1, examId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Submission> submissions = new ArrayList<>();
                while (rows.next()) {
                    submissions.add(new Submission(rows.getLong("id"), rows.getLong("exam_id"),
                            rows.getLong("student_id"), rows.getTimestamp("submitted_at").toLocalDateTime(),
                            rows.getString("status"), (Double) rows.getObject("score")));
                }
                return submissions;
            }
        }
    }

    public List<Submission> findPublishedForStudent(long studentId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT s.id, s.exam_id, s.student_id, s.submitted_at, s.status, s.score "
                        + "FROM submissions s JOIN exams e ON e.id = s.exam_id "
                        + "WHERE s.student_id = ? AND e.grades_published = TRUE ORDER BY s.id")) {
            statement.setLong(1, studentId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Submission> results = new ArrayList<>();
                while (rows.next()) {
                    results.add(new Submission(rows.getLong("id"), rows.getLong("exam_id"),
                            rows.getLong("student_id"), rows.getTimestamp("submitted_at").toLocalDateTime(),
                            rows.getString("status"), (Double) rows.getObject("score")));
                }
                return results;
            }
        }
    }

    public Submission save(Connection connection, Submission submission) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO submissions (exam_id, student_id, submitted_at, status, score) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING id")) {
            statement.setLong(1, submission.getExamId());
            statement.setLong(2, submission.getStudentId());
            statement.setTimestamp(3, Timestamp.valueOf(submission.getSubmittedAt()));
            statement.setString(4, submission.getStatus());
            if (submission.getScore() == null) statement.setNull(5, Types.DOUBLE);
            else statement.setDouble(5, submission.getScore());
            try (ResultSet keys = statement.executeQuery()) {
                if (!keys.next()) throw new SQLException("Submission insert returned no ID");
                submission.setId(keys.getLong(1));
                return submission;
            }
        }
    }
}

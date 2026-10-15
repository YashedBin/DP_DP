package com.eoms.repository;

import com.eoms.model.Answer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AnswerRepository {
    public Answer save(Connection connection, Answer answer) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO answers (submission_id, question_id, answer_text) VALUES (?, ?, ?) RETURNING id")) {
            statement.setLong(1, answer.getSubmissionId());
            statement.setLong(2, answer.getQuestionId());
            statement.setString(3, answer.getAnswerText());
            try (ResultSet keys = statement.executeQuery()) {
                if (!keys.next()) throw new SQLException("Answer insert returned no ID");
                answer.setId(keys.getLong(1));
                return answer;
            }
        }
    }

    public List<Answer> findBySubmissionId(Connection connection, long submissionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, submission_id, question_id, answer_text FROM answers "
                        + "WHERE submission_id = ? ORDER BY id")) {
            statement.setLong(1, submissionId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Answer> answers = new ArrayList<>();
                while (rows.next()) {
                    answers.add(new Answer(rows.getLong("id"), rows.getLong("submission_id"),
                            rows.getLong("question_id"), rows.getString("answer_text")));
                }
                return answers;
            }
        }
    }
}

package com.eoms.repository;

import com.eoms.entity.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class AnswerRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnswerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Answer> ROW_MAPPER = (rs, rowNum) -> new Answer(
            rs.getLong("id"),
            rs.getLong("submission_id"),
            rs.getLong("question_id"),
            rs.getString("answer_text")
    );

    public List<Answer> findBySubmissionId(Long submissionId) {
        return jdbcTemplate.query("SELECT * FROM answers WHERE submission_id = ?", ROW_MAPPER, submissionId);
    }

    public Answer save(Answer answer) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO answers (submission_id, question_id, answer_text) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setLong(1, answer.getSubmissionId());
            ps.setLong(2, answer.getQuestionId());
            ps.setString(3, answer.getAnswerText());
            return ps;
        }, keyHolder);
        answer.setId(keyHolder.getKey().longValue());
        return answer;
    }
}

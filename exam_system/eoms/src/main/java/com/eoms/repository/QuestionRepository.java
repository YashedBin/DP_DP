package com.eoms.repository;

import com.eoms.entity.Question;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class QuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Question> ROW_MAPPER = (rs, rowNum) -> new Question(
            rs.getLong("id"),
            rs.getLong("exam_id"),
            rs.getString("type"),
            rs.getString("text"),
            rs.getInt("marks")
    );

    public List<Question> findByExamId(Long examId) {
        return jdbcTemplate.query("SELECT * FROM questions WHERE exam_id = ?", ROW_MAPPER, examId);
    }

    public Question save(Question question) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO questions (exam_id, type, text, marks) VALUES (?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setLong(1, question.getExamId());
            ps.setString(2, question.getType());
            ps.setString(3, question.getText());
            ps.setInt(4, question.getMarks());
            return ps;
        }, keyHolder);
        question.setId(keyHolder.getKey().longValue());
        return question;
    }
}

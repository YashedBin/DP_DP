package com.eoms.repository;

import com.eoms.entity.Submission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class SubmissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Submission> ROW_MAPPER = (rs, rowNum) -> new Submission(
            rs.getLong("id"),
            rs.getLong("exam_id"),
            rs.getLong("student_id"),
            rs.getTimestamp("submitted_at").toLocalDateTime(),
            rs.getString("status"),
            (Double) rs.getObject("score")
    );

    public boolean existsByExamAndStudent(Long examId, Long studentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM submissions WHERE exam_id = ? AND student_id = ?",
                Integer.class, examId, studentId
        );
        return count != null && count > 0;
    }

    public List<Submission> findByExamId(Long examId) {
        return jdbcTemplate.query("SELECT * FROM submissions WHERE exam_id = ?", ROW_MAPPER, examId);
    }

    public Submission save(Submission submission) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO submissions (exam_id, student_id, submitted_at, status, score) VALUES (?, ?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setLong(1, submission.getExamId());
            ps.setLong(2, submission.getStudentId());
            ps.setTimestamp(3, Timestamp.valueOf(submission.getSubmittedAt()));
            ps.setString(4, submission.getStatus());
            if (submission.getScore() != null) {
                ps.setDouble(5, submission.getScore());
            } else {
                ps.setNull(5, java.sql.Types.DOUBLE);
            }
            return ps;
        }, keyHolder);
        submission.setId(keyHolder.getKey().longValue());
        return submission;
    }
}

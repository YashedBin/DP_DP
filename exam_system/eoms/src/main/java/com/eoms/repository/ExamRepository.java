package com.eoms.repository;

import com.eoms.entity.Exam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class ExamRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExamRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Exam> ROW_MAPPER = (rs, rowNum) -> new Exam(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getInt("duration_minutes"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getTimestamp("end_time").toLocalDateTime(),
            rs.getString("status")
    );

    public List<Exam> findAll() {
        return jdbcTemplate.query("SELECT * FROM exams", ROW_MAPPER);
    }

    public Exam findById(Long id) {
        List<Exam> results = jdbcTemplate.query("SELECT * FROM exams WHERE id = ?", ROW_MAPPER, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public Exam save(Exam exam) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO exams (title, duration_minutes, start_time, end_time, status) VALUES (?, ?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, exam.getTitle());
            ps.setInt(2, exam.getDurationMinutes());
            ps.setTimestamp(3, Timestamp.valueOf(exam.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(exam.getEndTime()));
            ps.setString(5, exam.getStatus());
            return ps;
        }, keyHolder);
        exam.setId(keyHolder.getKey().longValue());
        return exam;
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE exams SET status = ? WHERE id = ?", status, id);
    }
}

package com.eoms.repository;

import com.eoms.entity.Teacher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class TeacherRepository {

    private final JdbcTemplate jdbcTemplate;

    public TeacherRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Teacher> ROW_MAPPER = (rs, rowNum) -> new Teacher(
            rs.getLong("id"),
            rs.getString("username")
    );

    public Teacher findByUsername(String username) {
        List<Teacher> results = jdbcTemplate.query("SELECT * FROM teachers WHERE username = ?", ROW_MAPPER, username);
        return results.isEmpty() ? null : results.get(0);
    }

    public Teacher save(Teacher teacher) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO teachers (username) VALUES (?)",
                    new String[]{"id"}
            );
            ps.setString(1, teacher.getUsername());
            return ps;
        }, keyHolder);
        teacher.setId(keyHolder.getKey().longValue());
        return teacher;
    }
}
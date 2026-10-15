package com.eoms.repository;

import com.eoms.entity.Student;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class StudentRepository {

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Student> ROW_MAPPER = (rs, rowNum) -> new Student(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email")
    );

    public List<Student> findAll() {
        return jdbcTemplate.query("SELECT * FROM students", ROW_MAPPER);
    }

    public Student findById(Long id) {
        List<Student> results = jdbcTemplate.query("SELECT * FROM students WHERE id = ?", ROW_MAPPER, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public Student save(Student student) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO students (name, email) VALUES (?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, student.getName());
            ps.setString(2, student.getEmail());
            return ps;
        }, keyHolder);
        student.setId(keyHolder.getKey().longValue());
        return student;
    }

    // creates a placeholder row for this exact roll number if one doesn't exist yet;
    // does nothing if it already does. Safe to call every login.
    public void ensureExists(Long id) {
        jdbcTemplate.update(
                "INSERT INTO students (id, name, email) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
                id, "Student " + id, "student" + id + "@placeholder.local"
        );
    }
}
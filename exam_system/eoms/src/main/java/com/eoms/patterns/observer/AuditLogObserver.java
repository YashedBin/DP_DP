package com.eoms.patterns.observer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public class AuditLogObserver implements ExamEventListener {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogObserver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onExamEvent(ExamEvent event) {
        jdbcTemplate.update(
                "INSERT INTO audit_log (event_type, exam_id, student_id, logged_at, details) VALUES (?, ?, ?, ?, ?)",
                event.getType().name(),
                event.getExamId(),
                event.getStudentId(),
                Timestamp.valueOf(event.getOccurredAt()),
                event.getDetails()
        );
    }
}

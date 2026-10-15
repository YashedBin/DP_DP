package com.eoms.service;

import com.eoms.model.Exam;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Exam operations; drafting works without a database connection. */
public interface ExamService {
    Exam prepareDraft(String title, int durationMinutes, LocalDateTime startTime);

    Exam create(Exam draft) throws SQLException;

    Exam findById(long id) throws SQLException;

    List<Exam> findAll() throws SQLException;

    void start(long id) throws SQLException;

    void end(long id) throws SQLException;
}

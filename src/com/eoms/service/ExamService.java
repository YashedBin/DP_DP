package com.eoms.service;

import com.eoms.model.Exam;
import com.eoms.patterns.memento.ExamDraft;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Exam operations; drafting works without a database connection. */
public interface ExamService {
    Exam prepareDraft(String title, int durationMinutes, LocalDateTime startTime);

    Exam create(TeacherSession teacher, Exam draft) throws SQLException;

    Exam findById(long id) throws SQLException;

    List<Exam> findAll() throws SQLException;

    void start(TeacherSession teacher, long id) throws SQLException;

    void end(TeacherSession teacher, long id) throws SQLException;

    Exam duplicate(TeacherSession teacher, long id) throws SQLException;

    void restoreDraft(TeacherSession teacher, long id, ExamDraft draft) throws SQLException;

    void configureDraft(TeacherSession teacher, long id, Exam draft) throws SQLException;
}

package com.eoms.service;

import com.eoms.model.Exam;
import java.time.LocalDateTime;

/** Prepares an unsaved exam draft; persistence will be added with JDBC. */
public interface ExamService {
    Exam prepareDraft(String title, int durationMinutes, LocalDateTime startTime);
}

package com.eoms.patterns.chainofresponsibility;

import com.eoms.model.Exam;
import com.eoms.service.StudentSession;
import java.sql.Connection;
import java.time.LocalDateTime;

public record SubmissionContext(Connection connection, Exam exam, StudentSession student,
                                LocalDateTime now) {}

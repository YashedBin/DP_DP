package patterns.chainofresponsibility;

import model.Exam;
import service.StudentSession;
import java.sql.Connection;
import java.time.LocalDateTime;

public record SubmissionContext(Connection connection, Exam exam, StudentSession student,
                                LocalDateTime now) {}

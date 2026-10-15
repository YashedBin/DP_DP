package patterns.observer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

public final class AuditLogObserver implements ExamEventListener {
    @Override
    public void onEvent(ExamEvent event, Connection transaction) throws SQLException {
        try (PreparedStatement statement = transaction.prepareStatement(
                "INSERT INTO audit_log (event_type, exam_id, student_id, logged_at, details) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, event.type().name());
            statement.setLong(2, event.examId());
            if (event.studentId() == null) statement.setNull(3, Types.BIGINT);
            else statement.setLong(3, event.studentId());
            statement.setTimestamp(4, Timestamp.valueOf(event.occurredAt()));
            statement.setString(5, event.type().name());
            statement.executeUpdate();
        }
    }
}

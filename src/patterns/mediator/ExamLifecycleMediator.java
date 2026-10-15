package patterns.mediator;

import database.DatabaseConnection;
import model.Exam;
import patterns.bridge.Notification;
import patterns.observer.ExamEvent;
import patterns.observer.ExamEventPublisher;
import repository.ExamRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;

/** Coordinates state storage, audit events and post-commit notifications. */
public final class ExamLifecycleMediator {
    private final DatabaseConnection database;
    private final ExamRepository exams;
    private final ExamEventPublisher events;
    private final Notification notifications;
    private final Clock clock;

    public ExamLifecycleMediator(DatabaseConnection database, ExamRepository exams,
            ExamEventPublisher events, Notification notifications, Clock clock) {
        this.database = database;
        this.exams = exams;
        this.events = events;
        this.notifications = notifications;
        this.clock = clock;
    }

    public void start(long id) throws SQLException { change(id, "DRAFT", "ONGOING", ExamEvent.Type.EXAM_STARTED); }
    public void end(long id) throws SQLException { change(id, "ONGOING", "ENDED", ExamEvent.Type.EXAM_ENDED); }

    private void change(long id, String expected, String next, ExamEvent.Type event) throws SQLException {
        ExamEvent occurred;
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                Exam exam = exams.lockById(connection, id);
                if (exam == null || !expected.equals(exam.getStatus())) {
                    throw new IllegalStateException("Exam cannot transition from its current status");
                }
                LocalDateTime now = LocalDateTime.now(clock);
                if (event == ExamEvent.Type.EXAM_STARTED &&
                        (now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime()))) {
                    throw new IllegalStateException("Exam is outside its scheduled window");
                }
                if (!exams.transition(connection, id, expected, next)) {
                    throw new IllegalStateException("Exam state changed concurrently");
                }
                occurred = new ExamEvent(event, id, null, now);
                events.publish(occurred, connection);
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException failure) { ex.addSuppressed(failure); }
                throw ex;
            }
        }
        events.afterCommit(occurred);
        notifications.send("teacher", "Exam #" + id + " is " + next);
    }
}

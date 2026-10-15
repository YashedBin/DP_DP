package service;

import database.DatabaseConnection;
import model.Question;
import patterns.strategy.GradingStrategy;
import patterns.strategy.ManualStrategy;
import patterns.strategy.McqStrategy;
import patterns.templatemethod.GradingWorkflow;
import patterns.observer.ExamEvent;
import patterns.observer.ExamEventPublisher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Template Method defines the flow; Strategy chooses per-question evaluation. */
public final class GradingService extends GradingWorkflow<List<GradingService.Row>, List<Double>> {
    private final DatabaseConnection database;
    private final ExamEventPublisher events;
    private final Clock clock;
    private final Map<String, GradingStrategy> strategies = Map.of(
            "MCQ", new McqStrategy(), "SUBJECTIVE", new ManualStrategy(), "CODING", new ManualStrategy());

    public GradingService(DatabaseConnection database, ExamEventPublisher events, Clock clock) {
        this.database = database;
        this.events = events;
        this.clock = clock;
    }

    public Result gradeForTeacher(TeacherSession teacher, long submissionId) throws SQLException {
        requireTeacher(teacher);
        return grade(submissionId);
    }

    @Override
    protected void validate(long submissionId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM submissions s JOIN exams e ON e.id = s.exam_id "
                        + "WHERE s.id = ? AND NOT e.grades_published")) {
            statement.setLong(1, submissionId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("Submission missing or grades already published");
            }
        }
    }

    public record Row(Question question, String response, String key, Double manualMark) {}

    @Override
    protected List<Row> loadAnswers(long submissionId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT q.id, q.exam_id, q.type, q.text, q.marks, q.options, "
                        + "a.answer_text, a.awarded_marks, k.correct_answer "
                        + "FROM answers a JOIN questions q ON q.id = a.question_id "
                        + "LEFT JOIN question_keys k ON k.question_id = q.id "
                        + "WHERE a.submission_id = ? ORDER BY a.id")) {
            statement.setLong(1, submissionId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Row> loaded = new ArrayList<>();
                while (rows.next()) {
                    Question question = new Question(rows.getLong("id"), rows.getLong("exam_id"),
                            rows.getString("type"), rows.getString("text"), rows.getInt("marks"),
                            rows.getString("options"));
                    loaded.add(new Row(question, rows.getString("answer_text"),
                            rows.getString("correct_answer"), (Double) rows.getObject("awarded_marks")));
                }
                return loaded;
            }
        }
    }

    @Override
    protected List<Double> evaluate(List<Row> loaded) {
        List<Double> marks = new ArrayList<>();
        for (Row row : loaded) {
            GradingStrategy strategy = strategies.get(row.question.getType());
            if (strategy == null) throw new IllegalArgumentException("Unsupported question type");
            marks.add(strategy.evaluate(row.question, row.response, row.key, row.manualMark));
        }
        return marks;
    }

    @Override
    protected Double calculateScore(List<Double> marks) {
        if (marks.isEmpty() || marks.contains(null)) return null;
        return marks.stream().mapToDouble(Double::doubleValue).sum();
    }

    @Override
    protected Result produceResult(long submissionId, Double score) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE submissions s SET score = ?, status = ? FROM exams e "
                        + "WHERE s.id = ? AND e.id = s.exam_id AND NOT e.grades_published")) {
            if (score == null) statement.setNull(1, Types.DOUBLE);
            else statement.setDouble(1, score);
            statement.setString(2, score == null ? "NEEDS_REVIEW" : "GRADED");
            statement.setLong(3, submissionId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Submission missing or grades already published");
            }
        }
        return new Result(submissionId, score, score == null);
    }

    public void recordManualMark(TeacherSession teacher, long answerId, double marks) throws SQLException {
        requireTeacher(teacher);
        if (!Double.isFinite(marks) || marks < 0) throw new IllegalArgumentException("Invalid marks");
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE answers a SET awarded_marks = ? FROM questions q, exams e "
                        + "WHERE a.id = ? AND a.question_id = q.id AND e.id = q.exam_id "
                        + "AND NOT e.grades_published AND q.type IN ('SUBJECTIVE','CODING') "
                        + "AND ? <= q.marks")) {
            statement.setDouble(1, marks);
            statement.setLong(2, answerId);
            statement.setDouble(3, marks);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Answer not found or mark exceeds maximum");
            }
        }
    }

    public void setMcqKey(TeacherSession teacher, long questionId, String answer) throws SQLException {
        requireTeacher(teacher);
        if (answer == null || answer.isBlank()) throw new IllegalArgumentException("Correct answer is required");
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO question_keys (question_id, correct_answer) "
                        + "SELECT q.id, ? FROM questions q JOIN exams e ON e.id = q.exam_id "
                        + "WHERE q.id = ? AND q.type = 'MCQ' AND e.status = 'DRAFT' "
                        + "ON CONFLICT (question_id) DO UPDATE SET correct_answer = EXCLUDED.correct_answer")) {
            statement.setString(1, answer.trim());
            statement.setLong(2, questionId);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("MCQ question not found");
        }
    }

    public void publish(TeacherSession teacher, long examId) throws SQLException {
        requireTeacher(teacher);
        ExamEvent event = new ExamEvent(ExamEvent.Type.EXAM_PUBLISHED, examId, null,
                LocalDateTime.now(clock));
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE exams e SET grades_published = TRUE WHERE e.id = ? AND e.status = 'ENDED' "
                        + "AND NOT EXISTS (SELECT 1 FROM submissions s WHERE s.exam_id = e.id "
                        + "AND s.status <> 'GRADED')")) {
                statement.setLong(1, examId);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("End the exam and grade every submission before publishing");
                }
                events.publish(event, connection);
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException failure) { ex.addSuppressed(failure); }
                throw ex;
            }
        }
        events.afterCommit(event);
    }

    private static void requireTeacher(TeacherSession teacher) {
        if (teacher == null) throw new IllegalArgumentException("Teacher login required");
    }
}

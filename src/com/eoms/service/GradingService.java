package com.eoms.service;

import com.eoms.config.DatabaseConnection;
import com.eoms.model.Question;
import com.eoms.patterns.strategy.GradingStrategy;
import com.eoms.patterns.strategy.ManualStrategy;
import com.eoms.patterns.strategy.McqStrategy;
import com.eoms.patterns.templatemethod.GradingWorkflow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Template Method defines the flow; Strategy chooses per-question evaluation. */
public final class GradingService extends GradingWorkflow<List<GradingService.Row>, List<Double>> {
    private final DatabaseConnection database;
    private final Map<String, GradingStrategy> strategies = Map.of(
            "MCQ", new McqStrategy(), "SUBJECTIVE", new ManualStrategy(), "CODING", new ManualStrategy());

    public GradingService(DatabaseConnection database) { this.database = database; }

    public Result gradeForTeacher(TeacherSession teacher, long submissionId) throws SQLException {
        requireTeacher(teacher);
        return grade(submissionId);
    }

    @Override
    protected void validate(long submissionId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM submissions WHERE id = ?")) {
            statement.setLong(1, submissionId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("Submission not found");
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
                        "UPDATE submissions SET score = ?, status = ? WHERE id = ?")) {
            if (score == null) statement.setNull(1, Types.DOUBLE);
            else statement.setDouble(1, score);
            statement.setString(2, score == null ? "NEEDS_REVIEW" : "GRADED");
            statement.setLong(3, submissionId);
            statement.executeUpdate();
        }
        return new Result(submissionId, score, score == null);
    }

    public void recordManualMark(TeacherSession teacher, long answerId, double marks) throws SQLException {
        requireTeacher(teacher);
        if (!Double.isFinite(marks) || marks < 0) throw new IllegalArgumentException("Invalid marks");
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE answers a SET awarded_marks = ? FROM questions q "
                        + "WHERE a.id = ? AND a.question_id = q.id AND q.type IN ('SUBJECTIVE','CODING') "
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
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE exams e SET grades_published = TRUE WHERE e.id = ? AND e.status = 'ENDED' "
                        + "AND NOT EXISTS (SELECT 1 FROM submissions s WHERE s.exam_id = e.id "
                        + "AND s.status <> 'GRADED')")) {
            statement.setLong(1, examId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("End the exam and grade every submission before publishing");
            }
        }
    }

    private static void requireTeacher(TeacherSession teacher) {
        if (teacher == null) throw new IllegalArgumentException("Teacher login required");
    }
}

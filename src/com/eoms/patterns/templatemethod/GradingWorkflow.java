package com.eoms.patterns.templatemethod;

import java.sql.SQLException;

/** Fixed procedure; concrete workflow loads and evaluates according to its data source. */
public abstract class GradingWorkflow<Loaded, Evaluated> {
    protected final Result grade(long submissionId) throws SQLException {
        validate(submissionId);
        Loaded loaded = loadAnswers(submissionId);
        Evaluated evaluated = evaluate(loaded);
        Double score = calculateScore(evaluated);
        return produceResult(submissionId, score);
    }

    protected abstract void validate(long submissionId) throws SQLException;
    protected abstract Loaded loadAnswers(long submissionId) throws SQLException;
    protected abstract Evaluated evaluate(Loaded loaded) throws SQLException;
    protected abstract Double calculateScore(Evaluated evaluated);
    protected abstract Result produceResult(long submissionId, Double score) throws SQLException;

    public record Result(long submissionId, Double score, boolean requiresReview) {}
}

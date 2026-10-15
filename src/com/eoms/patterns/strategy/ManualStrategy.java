package com.eoms.patterns.strategy;

import com.eoms.model.Question;

/** Subjective/coding responses are never automatically assessed. */
public final class ManualStrategy implements GradingStrategy {
    @Override
    public Double evaluate(Question question, String response, String answerKey, Double manualMark) {
        if (response == null || response.isBlank()) return 0.0;
        return manualMark;
    }
}

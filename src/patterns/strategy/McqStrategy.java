package patterns.strategy;

import model.Question;

public final class McqStrategy implements GradingStrategy {
    @Override
    public Double evaluate(Question question, String response, String answerKey, Double manualMark) {
        if (answerKey == null || answerKey.isBlank()) return null;
        return answerKey.trim().equalsIgnoreCase(response == null ? "" : response.trim())
                ? (double) question.getMarks() : 0.0;
    }
}

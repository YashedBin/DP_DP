package patterns.strategy;

import model.Question;

/** null means human review is needed; it never implies a zero score. */
public interface GradingStrategy {
    Double evaluate(Question question, String response, String answerKey, Double manualMark);
}

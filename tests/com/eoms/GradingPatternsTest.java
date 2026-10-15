package com.eoms;

import com.eoms.model.Exam;
import com.eoms.model.Question;
import com.eoms.patterns.chainofresponsibility.ExamWindowHandler;
import com.eoms.patterns.chainofresponsibility.SubmissionContext;
import com.eoms.patterns.strategy.ManualStrategy;
import com.eoms.patterns.strategy.McqStrategy;
import com.eoms.patterns.templatemethod.GradingWorkflow;
import com.eoms.service.PasswordHashes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class GradingPatternsTest {
    public static void main(String[] args) throws Exception {
        Question mcq = new Question(null, null, "MCQ", "Two plus two", 5, "3,4");
        McqStrategy strategy = new McqStrategy();
        assert strategy.evaluate(mcq, "4", "4", null) == 5.0;
        assert strategy.evaluate(mcq, "3", "4", null) == 0.0;
        assert strategy.evaluate(mcq, "4", null, null) == null;
        assert new ManualStrategy().evaluate(mcq, "some text", null, null) == null;
        assert new ManualStrategy().evaluate(mcq, "some text", null, 3.0) == 3.0;
        String stored = PasswordHashes.hash("correct-password");
        assert PasswordHashes.matches("correct-password", stored);
        assert !PasswordHashes.matches("wrong-password", stored);

        List<String> order = new ArrayList<>();
        class CheckWorkflow extends GradingWorkflow<String, Double> {
            public Result run(long id) throws Exception { return grade(id); }
            protected void validate(long id) { order.add("validate"); }
            protected String loadAnswers(long id) { order.add("load"); return "answer"; }
            protected Double evaluate(String value) { order.add("evaluate"); return 2.0; }
            protected Double calculateScore(Double value) { order.add("calculate"); return value; }
            protected Result produceResult(long id, Double score) {
                order.add("result"); return new Result(id, score, false);
            }
        }
        CheckWorkflow workflow = new CheckWorkflow();
        assert workflow.run(2).score() == 2.0;
        assert order.equals(List.of("validate", "load", "evaluate", "calculate", "result"));
        Exam exam = new Exam(1L, "Test", 60, LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 1, 11, 0), "ENDED");
        try {
            new ExamWindowHandler().handle(new SubmissionContext(null, exam, null,
                    LocalDateTime.of(2026, 1, 1, 10, 30)));
            throw new AssertionError("Ended exam accepted");
        } catch (IllegalStateException expected) { /* rejected by window handler */ }
        System.out.println("Strategy, Template Method and submission-window checks passed.");
    }

}

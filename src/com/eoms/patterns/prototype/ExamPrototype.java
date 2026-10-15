package com.eoms.patterns.prototype;

import com.eoms.model.Exam;
import com.eoms.model.Question;
import java.util.ArrayList;
import java.util.List;

/** Copies configuration and question content, never database identities or results. */
public final class ExamPrototype {
    private ExamPrototype() {}

    public static Copy duplicate(Exam source, List<Question> questions) {
        if (source == null || questions == null) throw new IllegalArgumentException("Source exam is required");
        Exam exam = new Exam(null, source.getTitle() + " (copy)", source.getDurationMinutes(),
                source.getStartTime(), source.getEndTime(), "DRAFT");
        List<Question> copies = new ArrayList<>();
        for (Question question : questions) {
            copies.add(new Question(null, null, question.getType(), question.getText(),
                    question.getMarks(), question.getOptions()));
        }
        return new Copy(exam, copies);
    }

    public record Copy(Exam exam, List<Question> questions) {}
}

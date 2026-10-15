package com.eoms;

import com.eoms.model.Exam;
import com.eoms.model.Question;
import com.eoms.patterns.builder.ExamBuilder;
import com.eoms.patterns.memento.DraftHistory;
import com.eoms.patterns.memento.ExamDraft;
import com.eoms.patterns.prototype.ExamPrototype;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public final class CreationalPatternsTest {
    public static void main(String[] args) {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC);
        Exam original = new ExamBuilder(clock).title("Exam").build();
        assert original.getDurationMinutes() == 60 && original.getId() == null;
        Question q = new Question(9L, 4L, "MCQ", "First question", 5, "A,B");
        ExamPrototype.Copy copy = ExamPrototype.duplicate(original, List.of(q));
        assert copy.exam().getId() == null && copy.questions().get(0).getId() == null;
        q.setText("Modified source");
        assert "First question".equals(copy.questions().get(0).getText());

        ExamDraft draft = new ExamDraft(original, copy.questions());
        draft.setAnswerKeys(java.util.Arrays.asList("A"));
        DraftHistory history = new DraftHistory();
        history.save("v1", draft);
        draft.exam().setTitle("Changed");
        draft.questions().get(0).setText("Changed question");
        history.restore("v1", draft);
        assert "Exam".equals(draft.exam().getTitle());
        assert "First question".equals(draft.questions().get(0).getText());
        assert draft.questions().get(0).getId() == null;
        assert "A".equals(draft.answerKeys().get(0));
        history.restore("v1", draft);
        assert "Exam".equals(draft.exam().getTitle());
        System.out.println("Builder, Prototype and Memento checks passed.");
    }
}

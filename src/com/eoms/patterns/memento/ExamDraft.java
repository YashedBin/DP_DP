package com.eoms.patterns.memento;

import com.eoms.model.Exam;
import com.eoms.model.Question;
import com.eoms.patterns.prototype.ExamPrototype;
import java.util.ArrayList;
import java.util.List;

/** Originator: editable, unsaved exam content. */
public final class ExamDraft {
    private Exam exam;
    private List<Question> questions;

    public ExamDraft(Exam exam, List<Question> questions) { restore(new Snapshot(exam, questions)); }

    public Exam exam() { return exam; }
    public List<Question> questions() { return questions; }
    public void setExam(Exam value) { exam = value; }
    public void setQuestions(List<Question> value) { questions = value; }

    public Snapshot capture() { return new Snapshot(exam, questions); }

    public void restore(Snapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("Snapshot is required");
        // Never carry an ID, status, grades or other persisted history into an editor draft.
        ExamPrototype.Copy copy = ExamPrototype.duplicate(snapshot.exam, snapshot.questions);
        Exam source = copy.exam();
        exam = new Exam(null, snapshot.exam.getTitle(), source.getDurationMinutes(),
                source.getStartTime(), source.getEndTime(), "DRAFT");
        questions = new ArrayList<>(copy.questions());
    }

    /** Memento: captures detached content; accessors return fresh detached copies. */
    public static final class Snapshot {
        private final Exam exam;
        private final List<Question> questions;

        private Snapshot(Exam exam, List<Question> questions) {
            ExamPrototype.Copy copy = ExamPrototype.duplicate(exam, questions);
            this.exam = copy.exam();
            this.exam.setTitle(exam.getTitle());
            this.questions = copy.questions();
        }
    }
}

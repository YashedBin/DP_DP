package patterns.memento;

import model.Exam;
import model.Question;
import patterns.prototype.ExamPrototype;
import java.util.ArrayList;
import java.util.List;

/** Originator: editable, unsaved exam content. */
public final class ExamDraft {
    private Exam exam;
    private List<Question> questions;
    private List<String> answerKeys;

    public ExamDraft(Exam exam, List<Question> questions) {
        List<String> blanks = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) blanks.add(null);
        restore(new Snapshot(exam, questions, blanks));
    }

    public Exam exam() { return exam; }
    public List<Question> questions() { return questions; }
    public List<String> answerKeys() { return new ArrayList<>(answerKeys); }
    public void setExam(Exam value) { exam = value; }
    public void setQuestions(List<Question> value) {
        questions = value;
        answerKeys = new ArrayList<>();
        for (int i = 0; i < value.size(); i++) answerKeys.add(null);
    }
    public void setAnswerKeys(List<String> values) {
        if (values.size() != questions.size()) throw new IllegalArgumentException("Question key count mismatch");
        answerKeys = new ArrayList<>(values);
    }

    public Snapshot capture() { return new Snapshot(exam, questions, answerKeys); }

    public void restore(Snapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("Snapshot is required");
        // Never carry an ID, status, grades or other persisted history into an editor draft.
        ExamPrototype.Copy copy = ExamPrototype.duplicate(snapshot.exam, snapshot.questions);
        Exam source = copy.exam();
        exam = new Exam(null, snapshot.exam.getTitle(), source.getDurationMinutes(),
                source.getStartTime(), source.getEndTime(), "DRAFT");
        questions = new ArrayList<>(copy.questions());
        answerKeys = new ArrayList<>(snapshot.answerKeys);
    }

    /** Memento: captures detached content; accessors return fresh detached copies. */
    public static final class Snapshot {
        private final Exam exam;
        private final List<Question> questions;
        private final List<String> answerKeys;

        private Snapshot(Exam exam, List<Question> questions, List<String> answerKeys) {
            ExamPrototype.Copy copy = ExamPrototype.duplicate(exam, questions);
            this.exam = copy.exam();
            this.exam.setTitle(exam.getTitle());
            this.questions = copy.questions();
            this.answerKeys = new ArrayList<>(answerKeys);
        }
    }
}

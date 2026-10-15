package com.eoms.ui;

import com.eoms.config.ApplicationContext;
import com.eoms.model.Exam;
import com.eoms.model.Question;
import com.eoms.patterns.memento.DraftHistory;
import com.eoms.patterns.memento.ExamDraft;
import com.eoms.service.TeacherSession;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/** Teacher-only question editor; snapshot/restore works on detached, unsaved draft content. */
public final class ExamEditorPanel extends JPanel {
    private final ApplicationContext context;
    private final TeacherSession teacher;
    private final long examId;
    private final DraftHistory history = new DraftHistory();
    private ExamDraft draft;
    private final JList<Question> list = new JList<>();

    public ExamEditorPanel(ApplicationContext context, TeacherSession teacher, Exam exam) {
        super(new BorderLayout(8, 8));
        this.context = context;
        this.teacher = teacher;
        this.examId = exam.getId();
        draft = new ExamDraft(exam, List.of());

        JComboBox<String> type = new JComboBox<>(new String[]{"MCQ", "SUBJECTIVE", "CODING"});
        JTextField text = new JTextField();
        JTextField marks = new JTextField("5");
        JTextField options = new JTextField();
        JTextField key = new JTextField();
        JPanel fields = new JPanel(new GridLayout(5, 2, 8, 8));
        fields.add(new JLabel("Type")); fields.add(type);
        fields.add(new JLabel("Question")); fields.add(text);
        fields.add(new JLabel("Marks")); fields.add(marks);
        fields.add(new JLabel("Options (comma-separated)")); fields.add(options);
        fields.add(new JLabel("MCQ correct option text")); fields.add(key);
        add(fields, BorderLayout.NORTH);

        list.setCellRenderer((items, value, index, selected, focus) -> {
            JLabel row = new JLabel(value == null ? "" : "#" + value.getId() + " [" + value.getType()
                    + "] " + value.getText() + " (" + value.getMarks() + ")");
            row.setOpaque(true);
            row.setBackground(selected ? items.getSelectionBackground() : items.getBackground());
            row.setForeground(selected ? items.getSelectionForeground() : items.getForeground());
            return row;
        });
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(2, 3, 6, 6));
        JButton refresh = new JButton("Load questions");
        JButton add = new JButton("Add question");
        JButton update = new JButton("Update selected");
        JButton remove = new JButton("Remove selected");
        JButton save = new JButton("Save snapshot");
        JButton restore = new JButton("Restore snapshot (preview)");
        for (JButton button : new JButton[]{refresh, add, update, remove, save, restore}) actions.add(button);
        add(actions, BorderLayout.SOUTH);

        refresh.addActionListener(e -> refresh());
        add.addActionListener(e -> {
            int value;
            try { value = Integer.parseInt(marks.getText().trim()); }
            catch (NumberFormatException ex) { UiTasks.show(this, "Marks must be numeric"); return; }
            String selectedType = (String) type.getSelectedItem();
            String answer = key.getText().trim();
            String prompt = text.getText();
            String choices = options.getText();
            UiTasks.run(this, () -> {
                Question created = context.questions().add(teacher, examId, selectedType,
                        prompt, value, choices);
                if ("MCQ".equals(selectedType) && !answer.isEmpty())
                    context.grading().setMcqKey(teacher, created.getId(), answer);
                return created;
            }, created -> { UiTasks.show(this, "Question #" + created.getId() + " saved"); refresh(); });
        });
        update.addActionListener(e -> {
            Question selected = list.getSelectedValue();
            if (selected == null) return;
            int value;
            try { value = Integer.parseInt(marks.getText().trim()); }
            catch (NumberFormatException ex) { UiTasks.show(this, "Marks must be numeric"); return; }
            Question edited = new Question(selected.getId(), examId, (String) type.getSelectedItem(),
                    text.getText(), value, options.getText());
            String answer = key.getText().trim();
            UiTasks.run(this, () -> {
                context.questions().update(teacher, edited);
                if ("MCQ".equals(edited.getType()) && !answer.isBlank())
                    context.grading().setMcqKey(teacher, edited.getId(), answer);
                return edited;
            }, ignored -> refresh());
        });
        remove.addActionListener(e -> {
            Question selected = list.getSelectedValue();
            if (selected == null) return;
            UiTasks.run(this, () -> { context.questions().remove(teacher, examId, selected.getId()); return null; },
                    ignored -> refresh());
        });
        list.addListSelectionListener(e -> {
            Question selected = list.getSelectedValue();
            if (selected == null || e.getValueIsAdjusting()) return;
            type.setSelectedItem(selected.getType());
            text.setText(selected.getText());
            marks.setText(Integer.toString(selected.getMarks()));
            options.setText(selected.getOptions() == null ? "" : selected.getOptions());
            key.setText(""); // Never reveal a stored answer key in a general question listing.
        });
        save.addActionListener(e -> {
            String version = javax.swing.JOptionPane.showInputDialog(this, "Snapshot name");
            if (version == null) return;
            try {
                history.save(version, draft);
                UiTasks.show(this, "Snapshot saved in memory for this editor session");
            } catch (IllegalArgumentException ex) { UiTasks.show(this, ex.getMessage()); }
        });
        restore.addActionListener(e -> {
            String version = javax.swing.JOptionPane.showInputDialog(this, "Snapshot name");
            if (version == null) return;
            try {
                history.restore(version, draft);
                UiTasks.run(this, () -> { context.exams().restoreDraft(teacher, examId, draft); return null; },
                        ignored -> {
                            UiTasks.show(this, "Draft questions and MCQ keys restored with new IDs.");
                            refresh();
                        });
            } catch (IllegalArgumentException ex) { UiTasks.show(this, ex.getMessage()); }
        });
        refresh();
    }

    public void refresh() {
        UiTasks.run(this, () -> {
            List<Question> found = context.questions().listForEditor(teacher, examId);
            var keys = context.questions().answerKeysForEditor(teacher, examId);
            List<String> aligned = new ArrayList<>();
            for (Question question : found) aligned.add(keys.get(question.getId()));
            return new QuestionData(found, aligned);
        }, data -> {
            draft.setQuestions(new ArrayList<>(data.questions()));
            draft.setAnswerKeys(data.keys());
            list.setListData(data.questions().toArray(Question[]::new));
        });
    }

    private record QuestionData(List<Question> questions, List<String> keys) {}
}

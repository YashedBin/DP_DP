package ui;

import config.ApplicationContext;
import model.Exam;
import model.Question;
import patterns.memento.DraftHistory;
import patterns.memento.ExamDraft;
import service.TeacherSession;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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
        super(new BorderLayout(0, 18));
        setBackground(AppTheme.BACKGROUND);
        setBorder(AppTheme.spacing(22, 26, 22, 26));
        this.context = context;
        this.teacher = teacher;
        this.examId = exam.getId();
        draft = new ExamDraft(exam, List.of());

        JComboBox<String> type = AppTheme.combo(new JComboBox<>(new String[]{"MCQ", "SUBJECTIVE", "CODING"}));
        JTextArea text = AppTheme.area(new JTextArea(3, 42));
        JTextField marks = AppTheme.field(new JTextField("5"));
        JTextField options = AppTheme.field(new JTextField());
        JTextField key = AppTheme.field(new JTextField());
        JPanel heading = AppTheme.transparent(new BorderLayout(0, 5));
        heading.add(AppTheme.title("Question editor"), BorderLayout.NORTH);
        heading.add(AppTheme.muted("" + exam.getTitle() + "  ·  Answer keys stay private."), BorderLayout.SOUTH);
        JPanel editor = AppTheme.surface(new BorderLayout(0, 14), 18);
        editor.add(AppTheme.section("Write a question"), BorderLayout.NORTH);
        JPanel fields = AppTheme.transparent(new GridLayout(4, 2, 12, 10));
        fields.add(AppTheme.section("Type")); fields.add(type);
        fields.add(AppTheme.section("Marks")); fields.add(marks);
        fields.add(AppTheme.section("Options · comma-separated")); fields.add(options);
        fields.add(AppTheme.section("Correct option · MCQ only")); fields.add(key);
        JPanel editorContent = AppTheme.transparent(new BorderLayout(0, 14));
        JPanel promptPanel = AppTheme.transparent(new BorderLayout(0, 6));
        promptPanel.add(AppTheme.section("Question text"), BorderLayout.NORTH);
        JScrollPane textScroll = AppTheme.scroll(text);
        textScroll.setPreferredSize(new java.awt.Dimension(320, 77));
        promptPanel.add(textScroll, BorderLayout.CENTER);
        editorContent.add(promptPanel, BorderLayout.NORTH);
        editorContent.add(fields, BorderLayout.CENTER);
        editor.add(editorContent, BorderLayout.CENTER);
        JPanel top = AppTheme.transparent(new BorderLayout(0, 16));
        top.add(heading, BorderLayout.NORTH);
        top.add(editor, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        list.setCellRenderer((items, value, index, selected, focus) -> {
            JPanel row = AppTheme.transparent(new BorderLayout(8, 0));
            row.setOpaque(true);
            row.setBackground(selected ? AppTheme.SELECTION : AppTheme.SURFACE);
            row.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                    AppTheme.spacing(10, 14, 10, 14)));
            String prompt = value == null ? "" : value.getText().replace('\n', ' ');
            row.add(AppTheme.section(prompt.length() > 85 ? prompt.substring(0, 85) + "…" : prompt), BorderLayout.CENTER);
            row.add(AppTheme.muted(value == null ? "" : value.getType() + "  ·  " + value.getMarks() + " marks"),
                    BorderLayout.EAST);
            return row;
        });
        list.setFixedCellHeight(52);
        list.setBackground(AppTheme.SURFACE);
        JPanel existing = AppTheme.transparent(new BorderLayout(0, 9));
        existing.add(AppTheme.section("Existing questions"), BorderLayout.NORTH);
        existing.add(AppTheme.scroll(list), BorderLayout.CENTER);
        add(existing, BorderLayout.CENTER);

        JPanel actions = AppTheme.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton refresh = new AppButton("Reload questions", AppButton.Kind.QUIET);
        JButton add = new AppButton("Add question", AppButton.Kind.PRIMARY);
        JButton update = new AppButton("Update selected", AppButton.Kind.SECONDARY);
        JButton remove = new AppButton("Remove selected", AppButton.Kind.DANGER);
        JButton save = new AppButton("Save snapshot", AppButton.Kind.QUIET);
        JButton restore = new AppButton("Restore snapshot", AppButton.Kind.QUIET);
        actions.add(add); actions.add(update); actions.add(remove); actions.add(refresh);
        JPanel snapshots = AppTheme.surface(new BorderLayout(10, 0), 10);
        JPanel description = AppTheme.transparent(new GridLayout(2, 1, 0, 4));
        description.add(AppTheme.section("Draft history"));
        description.add(AppTheme.muted("Session only · Restoring replaces draft questions."));
        snapshots.add(description, BorderLayout.CENTER);
        JPanel historyActions = AppTheme.transparent(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        historyActions.add(save); historyActions.add(restore);
        snapshots.add(historyActions, BorderLayout.EAST);
        JPanel bottom = AppTheme.transparent(new BorderLayout(0, 14));
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(snapshots, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

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

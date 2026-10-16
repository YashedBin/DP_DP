package ui;

import config.ApplicationContext;
import model.Exam;
import model.Question;
import service.StudentSession;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Spacious, scrollable examination paper; answer collection and submit flow stay unchanged. */
public final class ExamAttemptPanel extends JPanel {
    private final ApplicationContext context;
    private final StudentSession student;
    private final Exam exam;
    private final JPanel questions = AppTheme.transparent(null);
    private final Map<Long, java.util.function.Supplier<String>> responses = new LinkedHashMap<>();
    private final AppButton submit = new AppButton("Submit examination", AppButton.Kind.PRIMARY);
    private final JLabel remaining = AppTheme.muted("");
    private final JLabel progress = AppTheme.muted("Loading questions…");
    private final Timer countdown = new Timer(30_000, event -> updateRemaining());

    public ExamAttemptPanel(ApplicationContext context, StudentSession student, Exam exam) {
        super(new BorderLayout());
        this.context = context;
        this.student = student;
        this.exam = exam;
        setBackground(AppTheme.BACKGROUND);

        JPanel header = AppTheme.page(new BorderLayout(0, 20));
        JPanel upper = AppTheme.transparent(new BorderLayout());
        upper.add(AppTheme.brand(), BorderLayout.WEST);
        upper.add(remaining, BorderLayout.EAST);
        header.add(upper, BorderLayout.NORTH);
        JPanel title = AppTheme.transparent(new BorderLayout(0, 6));
        title.add(AppTheme.title(exam.getTitle()), BorderLayout.NORTH);
        title.add(AppTheme.muted("Examination paper   ·   " + exam.getDurationMinutes() + " minutes   ·   "
                + "Closes " + exam.getEndTime().format(DateTimeFormatter.ofPattern("EEE d MMM, h:mm a"))),
                BorderLayout.SOUTH);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        questions.setLayout(new BoxLayout(questions, BoxLayout.Y_AXIS));
        questions.setBorder(AppTheme.spacing(8, 32, 32, 32));
        JScrollPane paper = AppTheme.scroll(questions);
        paper.setBorder(null);
        paper.getViewport().setBackground(AppTheme.BACKGROUND);
        add(paper, BorderLayout.CENTER);

        JPanel footer = AppTheme.surface(new BorderLayout(16, 0), 16);
        footer.add(progress, BorderLayout.WEST);
        JPanel right = AppTheme.transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add(submit);
        footer.add(right, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        submit.setEnabled(false);
        submit.addActionListener(e -> {
            Map<Long, String> answers = new LinkedHashMap<>();
            responses.forEach((id, field) -> answers.put(id, field.get()));
            if (javax.swing.JOptionPane.showConfirmDialog(this,
                    "Submit your final answers? You cannot change them afterwards.",
                    "Confirm submission", javax.swing.JOptionPane.YES_NO_OPTION)
                    != javax.swing.JOptionPane.YES_OPTION) return;
            submit.setEnabled(false);
            UiTasks.run(this, () -> context.submissions().submit(exam.getId(), student, answers),
                    result -> UiTasks.show(this, "Submission #" + result.getId() + " received."));
        });
        updateRemaining();
        countdown.start();
        load();
    }

    @Override public void removeNotify() {
        countdown.stop();
        super.removeNotify();
    }

    private void updateRemaining() {
        long minutes = Math.max(0, Duration.between(LocalDateTime.now(), exam.getEndTime()).toMinutes());
        remaining.setText(minutes == 0 ? "Time window ending" : minutes + " min remaining");
    }

    private void load() {
        UiTasks.run(this, () -> context.questions().listForStudent(exam.getId(), student), this::display);
    }

    private void display(List<Question> loaded) {
        responses.clear();
        questions.removeAll();
        if (loaded.isEmpty()) {
            JPanel empty = AppTheme.surface(new BorderLayout(), 28);
            empty.add(AppTheme.section("There are no questions available for this examination."));
            questions.add(empty);
        }
        int number = 0;
        for (Question question : loaded) {
            JPanel section = AppTheme.surface(new BorderLayout(0, 17), 24);
            section.setAlignmentX(LEFT_ALIGNMENT);
            JPanel top = AppTheme.transparent(new BorderLayout());
            top.add(AppTheme.label("QUESTION " + (++number) + "  /  " + loaded.size(),
                    AppTheme.SMALL.deriveFont(Font.BOLD), AppTheme.ACCENT_DARK), BorderLayout.WEST);
            top.add(AppTheme.muted(question.getMarks() + " marks  ·  " + question.getType()), BorderLayout.EAST);
            section.add(top, BorderLayout.NORTH);

            JPanel content = AppTheme.transparent(new BorderLayout(0, 16));
            JTextArea prompt = AppTheme.area(new JTextArea(question.getText()));
            prompt.setFont(AppTheme.QUESTION);
            prompt.setForeground(AppTheme.INK);
            prompt.setEditable(false);
            prompt.setFocusable(false);
            prompt.setOpaque(false);
            prompt.setBorder(null);
            prompt.setRows(Math.max(2, (question.getText().length() / 90) + 1));
            content.add(prompt, BorderLayout.NORTH);

            if ("MCQ".equals(question.getType()) && question.getOptions() != null) {
                JPanel options = AppTheme.transparent(new GridLayout(0, 1, 0, 8));
                ButtonGroup group = new ButtonGroup();
                List<JRadioButton> choices = new java.util.ArrayList<>();
                for (String raw : question.getOptions().split(",")) {
                    String value = raw.trim();
                    JRadioButton choice = new JRadioButton(value);
                    choice.setFont(AppTheme.BODY);
                    choice.setForeground(AppTheme.INK);
                    choice.setBackground(AppTheme.SURFACE);
                    choice.setFocusPainted(true);
                    choice.setBorder(AppTheme.spacing(6, 8, 6, 8));
                    choice.addActionListener(e -> updateProgress());
                    group.add(choice);
                    options.add(choice);
                    choices.add(choice);
                }
                content.add(options, BorderLayout.CENTER);
                responses.put(question.getId(), () -> choices.stream().filter(JRadioButton::isSelected)
                        .map(JRadioButton::getText).findFirst().orElse(""));
            } else if ("CODING".equals(question.getType()) || "SUBJECTIVE".equals(question.getType())) {
                JTextArea answer = AppTheme.area(new JTextArea("CODING".equals(question.getType()) ? 7 : 5, 36));
                if ("CODING".equals(question.getType())) {
                    answer.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
                    answer.setLineWrap(false);
                }
                answer.getDocument().addDocumentListener(changed());
                JScrollPane input = AppTheme.scroll(answer);
                input.setPreferredSize(new Dimension(300, "CODING".equals(question.getType()) ? 138 : 105));
                content.add(input, BorderLayout.CENTER);
                responses.put(question.getId(), answer::getText);
            } else {
                JTextField answer = AppTheme.field(new JTextField());
                answer.getDocument().addDocumentListener(changed());
                content.add(answer, BorderLayout.CENTER);
                responses.put(question.getId(), answer::getText);
            }
            section.add(content, BorderLayout.CENTER);
            section.setMaximumSize(new Dimension(Integer.MAX_VALUE, section.getPreferredSize().height));
            questions.add(section);
            questions.add(Box.createVerticalStrut(18));
        }
        submit.setEnabled(!loaded.isEmpty());
        updateProgress();
        questions.revalidate();
        questions.repaint();
    }

    private DocumentListener changed() {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateProgress(); }
            @Override public void removeUpdate(DocumentEvent e) { updateProgress(); }
            @Override public void changedUpdate(DocumentEvent e) { updateProgress(); }
        };
    }

    private void updateProgress() {
        long answered = responses.values().stream().map(java.util.function.Supplier::get)
                .filter(value -> value != null && !value.isBlank()).count();
        progress.setText(answered + " of " + responses.size() + " answered");
    }
}

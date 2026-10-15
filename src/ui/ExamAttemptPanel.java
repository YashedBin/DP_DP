package ui;

import config.ApplicationContext;
import model.Exam;
import model.Question;
import service.StudentSession;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class ExamAttemptPanel extends JPanel {
    private final ApplicationContext context;
    private final StudentSession student;
    private final Exam exam;
    private final JPanel questions = new JPanel();
    private final Map<Long, java.util.function.Supplier<String>> responses = new LinkedHashMap<>();
    private final JButton submit = new JButton("Submit exam");

    public ExamAttemptPanel(ApplicationContext context, StudentSession student, Exam exam) {
        super(new BorderLayout(10, 10));
        this.context = context;
        this.student = student;
        this.exam = exam;
        add(new JLabel(exam.getTitle() + " — submit before " + exam.getEndTime()), BorderLayout.NORTH);
        questions.setLayout(new javax.swing.BoxLayout(questions, javax.swing.BoxLayout.Y_AXIS));
        add(new JScrollPane(questions), BorderLayout.CENTER);
        submit.setEnabled(false);
        add(submit, BorderLayout.SOUTH);
        submit.addActionListener(e -> {
            Map<Long, String> answers = new LinkedHashMap<>();
            responses.forEach((id, field) -> answers.put(id, field.get()));
            if (javax.swing.JOptionPane.showConfirmDialog(this, "Submit your final answers?",
                    "Confirm submission", javax.swing.JOptionPane.YES_NO_OPTION)
                    != javax.swing.JOptionPane.YES_OPTION) return;
            submit.setEnabled(false);
            UiTasks.run(this, () -> context.submissions().submit(exam.getId(), student, answers),
                    result -> UiTasks.show(this, "Submission #" + result.getId() + " received"));
        });
        load();
    }

    private void load() {
        UiTasks.run(this, () -> context.questions().listForStudent(exam.getId(), student), this::display);
    }

    private void display(List<Question> loaded) {
        responses.clear();
        questions.removeAll();
        for (Question question : loaded) {
            JPanel item = new JPanel(new GridLayout(2, 1, 4, 4));
            item.add(new JLabel(question.getText() + " [" + question.getType() + ", "
                    + question.getMarks() + " marks]"));
            if ("MCQ".equals(question.getType()) && question.getOptions() != null) {
                String[] raw = question.getOptions().split(",");
                String[] options = new String[raw.length + 1];
                options[0] = "Choose an answer";
                for (int i = 0; i < raw.length; i++) options[i + 1] = raw[i].trim();
                JComboBox<String> choice = new JComboBox<>(options);
                item.add(choice);
                responses.put(question.getId(), () -> choice.getSelectedIndex() == 0
                        ? "" : (String) choice.getSelectedItem());
            } else if ("CODING".equals(question.getType()) || "SUBJECTIVE".equals(question.getType())) {
                JTextArea answer = new JTextArea(3, 40);
                item.add(new JScrollPane(answer));
                responses.put(question.getId(), answer::getText);
            } else {
                JTextField answer = new JTextField();
                item.add(answer);
                responses.put(question.getId(), answer::getText);
            }
            questions.add(item);
        }
        if (loaded.isEmpty()) questions.add(new JLabel("This exam has no accessible questions"));
        submit.setEnabled(!loaded.isEmpty());
        questions.revalidate();
        questions.repaint();
    }
}

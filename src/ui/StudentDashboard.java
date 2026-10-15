package ui;

import config.ApplicationContext;
import model.Exam;
import model.Submission;
import service.StudentSession;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

public final class StudentDashboard extends JFrame {
    private final ApplicationContext context;
    private final StudentSession student;
    private final JList<Exam> exams = new JList<>();

    public StudentDashboard(ApplicationContext context, StudentSession student) {
        super("EOMS — Student dashboard #" + student.studentId());
        this.context = context;
        this.student = student;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.add(new JLabel("Available exams (select one to begin)"), BorderLayout.NORTH);
        exams.setCellRenderer((items, value, index, selected, focus) -> {
            JLabel row = new JLabel(value == null ? "" : "#" + value.getId() + " " + value.getTitle()
                    + " — closes " + value.getEndTime());
            row.setOpaque(true);
            row.setBackground(selected ? items.getSelectionBackground() : items.getBackground());
            row.setForeground(selected ? items.getSelectionForeground() : items.getForeground());
            return row;
        });
        root.add(new JScrollPane(exams), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 3, 8, 8));
        JButton refresh = new JButton("Refresh exams");
        JButton enter = new JButton("Enter exam");
        JButton grades = new JButton("Published grades");
        actions.add(refresh); actions.add(enter); actions.add(grades);
        root.add(actions, BorderLayout.SOUTH);
        context.notifications().showIn(message -> UiTasks.show(this, message));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { context.notifications().clear(); }
        });
        refresh.addActionListener(e -> refresh());
        enter.addActionListener(e -> {
            Exam selected = exams.getSelectedValue();
            if (selected == null) { UiTasks.show(this, "Select an exam"); return; }
            JFrame frame = new JFrame("Attempt: " + selected.getTitle());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ExamAttemptPanel(context, student, selected));
            frame.setSize(720, 500); frame.setLocationRelativeTo(this); frame.setVisible(true);
        });
        grades.addActionListener(e -> UiTasks.run(this,
                () -> context.students().publishedGrades(student), this::showGrades));
        setContentPane(root);
        setSize(680, 420);
        setLocationRelativeTo(null);
        refresh();
    }

    private void refresh() {
        UiTasks.run(this, () -> context.exams().findAll().stream()
                .filter(exam -> "ONGOING".equals(exam.getStatus()))
                .filter(exam -> !java.time.LocalDateTime.now().isBefore(exam.getStartTime())
                        && !java.time.LocalDateTime.now().isAfter(exam.getEndTime()))
                .toList(), results -> exams.setListData(results.toArray(Exam[]::new)));
    }

    private void showGrades(List<Submission> results) {
        if (results.isEmpty()) { UiTasks.show(this, "No published grades yet"); return; }
        StringBuilder text = new StringBuilder();
        for (Submission result : results) text.append("Exam #").append(result.getExamId())
                .append(" — ").append(result.getScore()).append('\n');
        UiTasks.show(this, text.toString());
    }
}

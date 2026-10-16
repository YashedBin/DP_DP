package ui;

import config.ApplicationContext;
import model.Exam;
import model.Submission;
import service.StudentSession;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class StudentDashboard extends JFrame {
    private static final DateTimeFormatter END_TIME = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a");
    private final ApplicationContext context;
    private final StudentSession student;
    private final JList<Exam> exams = new JList<>();

    public StudentDashboard(ApplicationContext context, StudentSession student) {
        super("YDVRC — Student");
        this.context = context;
        this.student = student;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = AppTheme.page(new BorderLayout(0, 22));

        JPanel header = AppTheme.transparent(new BorderLayout());
        header.add(AppTheme.brand(), BorderLayout.WEST);
        header.add(AppTheme.muted("Student #" + student.studentId()), BorderLayout.EAST);
        JPanel heading = AppTheme.transparent(new BorderLayout(0, 5));
        heading.add(AppTheme.title("Examinations"), BorderLayout.NORTH);
        heading.add(AppTheme.muted("Available now · Select an examination to begin"), BorderLayout.SOUTH);
        JPanel top = AppTheme.transparent(new BorderLayout(0, 34));
        top.add(header, BorderLayout.NORTH);
        top.add(heading, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        exams.setFixedCellHeight(86);
        exams.setBackground(AppTheme.SURFACE);
        exams.setSelectionBackground(AppTheme.SELECTION);
        exams.setCellRenderer((items, value, index, selected, focus) -> {
            JPanel row = AppTheme.transparent(new BorderLayout(16, 0));
            row.setOpaque(true);
            row.setBackground(selected ? AppTheme.SELECTION : AppTheme.SURFACE);
            row.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                    AppTheme.spacing(14, 19, 14, 19)));
            JPanel details = AppTheme.transparent(new GridLayout(2, 1, 0, 4));
            details.add(AppTheme.section(value == null ? "" : value.getTitle()));
            details.add(AppTheme.muted(value == null ? "" : "Ends " + value.getEndTime().format(END_TIME)
                    + "   ·   " + value.getDurationMinutes() + " minutes"));
            row.add(details, BorderLayout.CENTER);
            row.add(AppTheme.label("Enter  →", AppTheme.SMALL.deriveFont(java.awt.Font.BOLD), AppTheme.ACCENT),
                    BorderLayout.EAST);
            return row;
        });
        CardLayout states = new CardLayout();
        JPanel listing = AppTheme.transparent(states);
        listing.add(AppTheme.scroll(exams), "list");
        JPanel empty = AppTheme.surface(new GridLayout(2, 1, 0, 8), 28);
        empty.add(AppTheme.section("No examinations available right now"));
        empty.add(AppTheme.muted("Check again later, or refresh the list."));
        listing.add(empty, "empty");
        root.add(listing, BorderLayout.CENTER);

        JPanel actions = AppTheme.transparent(new BorderLayout());
        AppButton grades = new AppButton("Published grades", AppButton.Kind.QUIET);
        AppButton refresh = new AppButton("Refresh", AppButton.Kind.SECONDARY);
        AppButton enter = new AppButton("Enter examination", AppButton.Kind.PRIMARY);
        actions.add(grades, BorderLayout.WEST);
        JPanel right = AppTheme.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(refresh);
        right.add(enter);
        actions.add(right, BorderLayout.EAST);
        root.add(actions, BorderLayout.SOUTH);

        context.notifications().showIn(message -> UiTasks.show(this, message));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { context.notifications().clear(); }
        });
        refresh.addActionListener(e -> refresh(states, listing));
        enter.addActionListener(e -> {
            Exam selected = exams.getSelectedValue();
            if (selected == null) { UiTasks.show(this, "Choose an examination first."); return; }
            JFrame frame = new JFrame("Exam — " + selected.getTitle());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ExamAttemptPanel(context, student, selected));
            AppTheme.sizeWindow(frame, 880, 690, 560, 490);
            frame.setLocationRelativeTo(this);
            frame.setVisible(true);
        });
        exams.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && exams.getSelectedValue() != null) enter.doClick();
            }
        });
        grades.addActionListener(e -> UiTasks.run(this,
                () -> context.students().publishedGrades(student), this::showGrades));
        setContentPane(root);
        AppTheme.sizeWindow(this, 780, 560, 600, 420);
        refresh(states, listing);
    }

    private void refresh(CardLayout states, JPanel listing) {
        UiTasks.run(this, () -> context.exams().findAll().stream()
                .filter(exam -> "ONGOING".equals(exam.getStatus()))
                .filter(exam -> !java.time.LocalDateTime.now().isBefore(exam.getStartTime())
                        && !java.time.LocalDateTime.now().isAfter(exam.getEndTime()))
                .toList(), results -> {
                    exams.setListData(results.toArray(Exam[]::new));
                    states.show(listing, results.isEmpty() ? "empty" : "list");
                });
    }

    private void showGrades(List<Submission> results) {
        if (results.isEmpty()) { UiTasks.show(this, "No published grades yet."); return; }
        StringBuilder text = new StringBuilder();
        for (Submission result : results) text.append("Exam #").append(result.getExamId())
                .append("   ·   ").append(result.getScore()).append(" marks\n");
        UiTasks.show(this, text.toString());
    }
}

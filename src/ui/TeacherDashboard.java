package ui;

import config.ApplicationContext;
import model.Answer;
import model.Exam;
import model.Submission;
import patterns.observer.SwingRefreshObserver;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import service.TeacherSession;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public final class TeacherDashboard extends JFrame {
    private final ApplicationContext context;
    private final TeacherSession teacher;
    private final JList<Exam> exams = new JList<>();

    public TeacherDashboard(ApplicationContext context, TeacherSession teacher) {
        super("EOMS — Teacher dashboard");
        this.context = context;
        this.teacher = teacher;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel create = new JPanel(new GridLayout(3, 2, 6, 6));
        JTextField title = new JTextField();
        JTextField minutes = new JTextField("60");
        JTextField start = new JTextField(LocalDateTime.now().withSecond(0).withNano(0).toString());
        create.add(new JLabel("Title")); create.add(title);
        create.add(new JLabel("Duration (minutes)")); create.add(minutes);
        create.add(new JLabel("Start (YYYY-MM-DDTHH:MM)")); create.add(start);
        root.add(create, BorderLayout.NORTH);
        exams.setCellRenderer((items, value, index, selected, focus) -> {
            JLabel row = new JLabel(value == null ? "" : "#" + value.getId() + " " + value.getTitle()
                    + " — " + value.getStatus());
            row.setOpaque(true);
            row.setBackground(selected ? items.getSelectionBackground() : items.getBackground());
            row.setForeground(selected ? items.getSelectionForeground() : items.getForeground());
            return row;
        });
        root.add(new JScrollPane(exams), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(3, 3, 6, 6));
        JButton refresh = new JButton("Refresh exams");
        JButton add = new JButton("Create exam");
        JButton edit = new JButton("Edit questions");
        JButton duplicate = new JButton("Duplicate");
        JButton begin = new JButton("Start");
        JButton end = new JButton("End");
        JButton review = new JButton("Review/grade");
        JButton publish = new JButton("Publish grades");
        JButton configure = new JButton("Configure draft");
        for (JButton button : new JButton[]{refresh, add, configure, edit, duplicate, begin, end, review, publish})
            actions.add(button);
        root.add(actions, BorderLayout.SOUTH);

        refresh.addActionListener(e -> refresh());
        add.addActionListener(e -> {
            String name = title.getText();
            int length;
            LocalDateTime scheduled;
            try {
                length = Integer.parseInt(minutes.getText().trim());
                scheduled = LocalDateTime.parse(start.getText().trim());
            } catch (RuntimeException ex) { UiTasks.show(this, "Check duration and ISO start date/time"); return; }
            UiTasks.run(this, () -> context.exams().create(teacher,
                    context.exams().prepareDraft(name, length, scheduled)), created -> {
                UiTasks.show(this, "Created exam #" + created.getId()); refresh();
            });
        });
        edit.addActionListener(e -> {
            Exam exam = selected();
            if (exam == null) return;
            if (!"DRAFT".equals(exam.getStatus())) {
                UiTasks.show(this, "Questions can only be edited before the exam starts"); return;
            }
            JFrame frame = new JFrame("Edit #" + exam.getId());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ExamEditorPanel(context, teacher, exam));
            frame.setSize(760, 480); frame.setLocationRelativeTo(this); frame.setVisible(true);
        });
        duplicate.addActionListener(e -> {
            Exam exam = selected(); if (exam == null) return;
            UiTasks.run(this, () -> context.exams().duplicate(teacher, exam.getId()),
                    result -> { UiTasks.show(this, "Copied to exam #" + result.getId()); refresh(); });
        });
        configure.addActionListener(e -> {
            Exam current = selected(); if (current == null) return;
            if (!"DRAFT".equals(current.getStatus())) {
                UiTasks.show(this, "Only drafts can be configured"); return;
            }
            int length;
            LocalDateTime scheduled;
            try {
                length = Integer.parseInt(minutes.getText().trim());
                scheduled = LocalDateTime.parse(start.getText().trim());
            } catch (RuntimeException ex) { UiTasks.show(this, "Check duration and ISO start date/time"); return; }
            Exam draft = context.exams().prepareDraft(title.getText(), length, scheduled);
            UiTasks.run(this, () -> { context.exams().configureDraft(teacher, current.getId(), draft); return null; },
                    ignored -> refresh());
        });
        begin.addActionListener(e -> changeStatus(true));
        end.addActionListener(e -> changeStatus(false));
        review.addActionListener(e -> { Exam exam = selected(); if (exam != null) showReview(exam); });
        publish.addActionListener(e -> {
            Exam exam = selected(); if (exam == null) return;
            UiTasks.run(this, () -> { context.grading().publish(teacher, exam.getId()); return null; },
                    ignored -> UiTasks.show(this, "Grades published"));
        });
        SwingRefreshObserver observer = new SwingRefreshObserver(this::refresh);
        context.events().subscribeAfterCommit(observer);
        context.notifications().showIn(message -> UiTasks.show(this, message));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                context.events().unsubscribeAfterCommit(observer);
                context.notifications().clear();
            }
        });
        setContentPane(root);
        setSize(780, 500);
        setLocationRelativeTo(null);
        refresh();
    }

    private Exam selected() {
        Exam exam = exams.getSelectedValue();
        if (exam == null) UiTasks.show(this, "Select an exam first");
        return exam;
    }

    private void refresh() {
        UiTasks.run(this, () -> context.exams().findAll(), results -> exams.setListData(results.toArray(Exam[]::new)));
    }

    private void changeStatus(boolean starting) {
        Exam exam = selected(); if (exam == null) return;
        UiTasks.run(this, () -> {
            if (starting) context.exams().start(teacher, exam.getId());
            else context.exams().end(teacher, exam.getId());
            return null;
        }, ignored -> refresh());
    }

    private void showReview(Exam exam) {
        JFrame frame = new JFrame("Submissions for #" + exam.getId());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout());
        JList<Submission> list = new JList<>();
        list.setCellRenderer((items, value, index, selected, focus) -> {
            JLabel row = new JLabel(value == null ? "" : "#" + value.getId() + " student "
                    + value.getStudentId() + " " + value.getStatus() + " score: " + value.getScore());
            row.setOpaque(true);
            row.setBackground(selected ? items.getSelectionBackground() : items.getBackground());
            return row;
        });
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 3, 6, 6));
        JButton load = new JButton("Load answers");
        JButton mark = new JButton("Mark answer by ID");
        JButton grade = new JButton("Calculate grade");
        actions.add(load); actions.add(mark); actions.add(grade);
        panel.add(actions, BorderLayout.SOUTH);
        load.addActionListener(e -> {
            Submission submission = list.getSelectedValue(); if (submission == null) return;
            UiTasks.run(frame, () -> context.submissions().reviewAnswers(teacher, submission.getId()),
                    answers -> {
                        StringBuilder text = new StringBuilder();
                        for (Answer answer : answers) text.append("Answer #").append(answer.getId())
                                .append(" question #").append(answer.getQuestionId())
                                .append(": ").append(answer.getAnswerText()).append('\n');
                        UiTasks.show(frame, text.isEmpty() ? "No answers recorded" : text.toString());
                    });
        });
        mark.addActionListener(e -> {
            String id = javax.swing.JOptionPane.showInputDialog(frame, "Answer ID");
            if (id == null) return;
            String points = javax.swing.JOptionPane.showInputDialog(frame, "Marks (manual only)");
            if (points == null) return;
            try {
                long answerId = Long.parseLong(id.trim()); double score = Double.parseDouble(points.trim());
                UiTasks.run(frame, () -> { context.grading().recordManualMark(teacher, answerId, score); return null; },
                        ignored -> UiTasks.show(frame, "Manual mark saved; recalculate grade"));
            } catch (NumberFormatException ex) { UiTasks.show(frame, "Invalid number"); }
        });
        grade.addActionListener(e -> {
            Submission submission = list.getSelectedValue(); if (submission == null) return;
            UiTasks.run(frame, () -> context.grading().gradeForTeacher(teacher, submission.getId()),
                    result -> {
                        UiTasks.show(frame, result.requiresReview() ? "Still needs manual review or an MCQ key"
                                : "Grade calculated: " + result.score());
                        loadReviews(frame, list, exam.getId());
                    });
        });
        frame.add(panel); frame.setSize(680, 410); frame.setLocationRelativeTo(this); frame.setVisible(true);
        loadReviews(frame, list, exam.getId());
    }

    private void loadReviews(JFrame frame, JList<Submission> list, long examId) {
        UiTasks.run(frame, () -> context.submissions().listForReview(teacher, examId),
                results -> list.setListData(results.toArray(Submission[]::new)));
    }
}

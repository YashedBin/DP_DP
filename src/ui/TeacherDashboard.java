package ui;

import config.ApplicationContext;
import model.Answer;
import model.Exam;
import model.Submission;
import patterns.observer.SwingRefreshObserver;
import service.TeacherSession;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class TeacherDashboard extends JFrame {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM · h:mm a");
    private final ApplicationContext context;
    private final TeacherSession teacher;
    private final JList<Exam> exams = new JList<>();

    public TeacherDashboard(ApplicationContext context, TeacherSession teacher) {
        super("EOMS — Teacher");
        this.context = context;
        this.teacher = teacher;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = AppTheme.page(new BorderLayout(0, 21));

        JPanel header = AppTheme.transparent(new BorderLayout());
        header.add(AppTheme.brand(), BorderLayout.WEST);
        JPanel heading = AppTheme.transparent(new BorderLayout(16, 0));
        JPanel copy = AppTheme.transparent(new BorderLayout(0, 5));
        copy.add(AppTheme.title("Your examinations"), BorderLayout.NORTH);
        copy.add(AppTheme.muted("Prepare, conduct and review examinations in one place."), BorderLayout.SOUTH);
        heading.add(copy, BorderLayout.CENTER);
        AppButton add = new AppButton("+ New examination", AppButton.Kind.PRIMARY);
        heading.add(add, BorderLayout.EAST);
        JPanel top = AppTheme.transparent(new BorderLayout(0, 32));
        top.add(header, BorderLayout.NORTH);
        top.add(heading, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        exams.setFixedCellHeight(90);
        exams.setBackground(AppTheme.SURFACE);
        exams.setSelectionBackground(AppTheme.SELECTION);
        exams.setCellRenderer((items, value, index, selected, focus) -> {
            JPanel row = AppTheme.transparent(new BorderLayout(20, 0));
            row.setOpaque(true);
            row.setBackground(selected ? AppTheme.SELECTION : AppTheme.SURFACE);
            row.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                    AppTheme.spacing(15, 20, 15, 20)));
            JPanel details = AppTheme.transparent(new GridLayout(2, 1, 0, 5));
            details.add(AppTheme.section(value == null ? "" : value.getTitle()));
            details.add(AppTheme.muted(value == null ? "" : value.getDurationMinutes() + " minutes   ·   "
                    + value.getStartTime().format(WHEN) + "–" + value.getEndTime().format(WHEN)));
            row.add(details, BorderLayout.CENTER);
            row.add(AppTheme.status(value == null ? "" : value.getStatus()), BorderLayout.EAST);
            return row;
        });
        CardLayout states = new CardLayout();
        JPanel listing = AppTheme.transparent(states);
        listing.add(AppTheme.scroll(exams), "list");
        JPanel empty = AppTheme.surface(new GridLayout(2, 1, 0, 6), 30);
        empty.add(AppTheme.section("No examinations yet"));
        empty.add(AppTheme.muted("Create one to begin building a question paper."));
        listing.add(empty, "empty");
        root.add(listing, BorderLayout.CENTER);

        AppButton refresh = new AppButton("Refresh", AppButton.Kind.QUIET);
        AppButton edit = new AppButton("Edit questions", AppButton.Kind.PRIMARY);
        AppButton configure = new AppButton("Configure", AppButton.Kind.SECONDARY);
        AppButton duplicate = new AppButton("Duplicate", AppButton.Kind.QUIET);
        AppButton begin = new AppButton("Start exam", AppButton.Kind.SECONDARY);
        AppButton end = new AppButton("End exam", AppButton.Kind.SECONDARY);
        AppButton review = new AppButton("Review submissions", AppButton.Kind.SECONDARY);
        AppButton publish = new AppButton("Publish grades", AppButton.Kind.PRIMARY);
        JPanel actions = AppTheme.transparent(new BorderLayout(8, 0));
        JPanel contextual = AppTheme.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (AppButton button : new AppButton[]{edit, configure, duplicate, begin, end, review, publish})
            contextual.add(button);
        actions.add(contextual, BorderLayout.CENTER);
        JPanel headerActions = AppTheme.transparent(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.add(AppTheme.muted("Teacher workspace"));
        headerActions.add(refresh);
        header.add(headerActions, BorderLayout.EAST);
        root.add(actions, BorderLayout.SOUTH);
        Runnable updateActions = () -> {
            Exam selected = exams.getSelectedValue();
            boolean draft = selected != null && "DRAFT".equals(selected.getStatus());
            boolean ongoing = selected != null && "ONGOING".equals(selected.getStatus());
            boolean ended = selected != null && "ENDED".equals(selected.getStatus());
            edit.setVisible(draft);
            configure.setVisible(draft);
            begin.setVisible(draft);
            duplicate.setVisible(selected != null);
            end.setVisible(ongoing);
            review.setVisible(selected != null);
            publish.setVisible(ended);
            contextual.revalidate();
            contextual.repaint();
        };
        exams.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateActions.run(); });
        updateActions.run();

        JTextField title = AppTheme.field(new JTextField());
        JTextField minutes = AppTheme.field(new JTextField("60"));
        JTextField start = AppTheme.field(new JTextField(
                LocalDateTime.now().withSecond(0).withNano(0).toString()));
        add.addActionListener(e -> {
            if (!examForm("New examination", title, minutes, start)) return;
            String name = title.getText();
            int length;
            LocalDateTime scheduled;
            try {
                length = Integer.parseInt(minutes.getText().trim());
                scheduled = LocalDateTime.parse(start.getText().trim());
            } catch (RuntimeException ex) { UiTasks.show(this, "Check duration and ISO start date/time."); return; }
            UiTasks.run(this, () -> context.exams().create(teacher,
                    context.exams().prepareDraft(name, length, scheduled)), created -> {
                UiTasks.show(this, "Examination #" + created.getId() + " created.");
                refresh(states, listing, updateActions);
            });
        });
        configure.addActionListener(e -> {
            Exam current = selected(); if (current == null) return;
            title.setText(current.getTitle());
            minutes.setText(Integer.toString(current.getDurationMinutes()));
            start.setText(current.getStartTime().toString());
            if (!examForm("Configure draft", title, minutes, start)) return;
            int length;
            LocalDateTime scheduled;
            try {
                length = Integer.parseInt(minutes.getText().trim());
                scheduled = LocalDateTime.parse(start.getText().trim());
            } catch (RuntimeException ex) { UiTasks.show(this, "Check duration and ISO start date/time."); return; }
            Exam draft;
            try { draft = context.exams().prepareDraft(title.getText(), length, scheduled); }
            catch (IllegalArgumentException ex) { UiTasks.show(this, ex.getMessage()); return; }
            UiTasks.run(this, () -> { context.exams().configureDraft(teacher, current.getId(), draft); return null; },
                    ignored -> refresh(states, listing, updateActions));
        });
        edit.addActionListener(e -> {
            Exam exam = selected(); if (exam == null) return;
            if (!"DRAFT".equals(exam.getStatus())) {
                UiTasks.show(this, "Questions can only be edited before the exam starts."); return;
            }
            JFrame frame = new JFrame("Question editor — " + exam.getTitle());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ExamEditorPanel(context, teacher, exam));
            AppTheme.sizeWindow(frame, 900, 700, 760, 560);
            frame.setLocationRelativeTo(this);
            frame.setVisible(true);
        });
        duplicate.addActionListener(e -> {
            Exam exam = selected(); if (exam == null) return;
            UiTasks.run(this, () -> context.exams().duplicate(teacher, exam.getId()),
                    result -> { UiTasks.show(this, "Copied to examination #" + result.getId() + ".");
                        refresh(states, listing, updateActions); });
        });
        begin.addActionListener(e -> changeStatus(true, states, listing, updateActions));
        end.addActionListener(e -> changeStatus(false, states, listing, updateActions));
        review.addActionListener(e -> { Exam exam = selected(); if (exam != null) showReview(exam); });
        publish.addActionListener(e -> {
            Exam exam = selected(); if (exam == null) return;
            UiTasks.run(this, () -> { context.grading().publish(teacher, exam.getId()); return null; },
                    ignored -> UiTasks.show(this, "Grades are now published."));
        });
        refresh.addActionListener(e -> refresh(states, listing, updateActions));

        SwingRefreshObserver observer = new SwingRefreshObserver(() -> refresh(states, listing, updateActions));
        context.events().subscribeAfterCommit(observer);
        context.notifications().showIn(message -> UiTasks.show(this, message));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                context.events().unsubscribeAfterCommit(observer);
                context.notifications().clear();
            }
        });
        setContentPane(root);
        AppTheme.sizeWindow(this, 960, 660, 680, 470);
        refresh(states, listing, updateActions);
    }

    private boolean examForm(String caption, JTextField title, JTextField minutes, JTextField start) {
        JPanel form = AppTheme.surface(new GridLayout(3, 2, 12, 12), 16);
        form.add(AppTheme.section("Title")); form.add(title);
        form.add(AppTheme.section("Duration (minutes)")); form.add(minutes);
        form.add(AppTheme.section("Start (YYYY-MM-DDTHH:MM)")); form.add(start);
        return JOptionPane.showConfirmDialog(this, form, caption,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private Exam selected() {
        Exam exam = exams.getSelectedValue();
        if (exam == null) UiTasks.show(this, "Select an examination first.");
        return exam;
    }

    private void refresh(CardLayout states, JPanel listing, Runnable updateActions) {
        Long selectedId = exams.getSelectedValue() == null ? null : exams.getSelectedValue().getId();
        UiTasks.run(this, () -> context.exams().findAll(), results -> {
            exams.setListData(results.toArray(Exam[]::new));
            if (selectedId != null) {
                for (int i = 0; i < results.size(); i++) {
                    if (selectedId.equals(results.get(i).getId())) { exams.setSelectedIndex(i); break; }
                }
            }
            states.show(listing, results.isEmpty() ? "empty" : "list");
            updateActions.run();
        });
    }

    private void changeStatus(boolean starting, CardLayout states, JPanel listing, Runnable updateActions) {
        Exam exam = selected(); if (exam == null) return;
        UiTasks.run(this, () -> {
            if (starting) context.exams().start(teacher, exam.getId());
            else context.exams().end(teacher, exam.getId());
            return null;
        }, ignored -> refresh(states, listing, updateActions));
    }

    private void showReview(Exam exam) {
        JFrame frame = new JFrame("Review — " + exam.getTitle());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = AppTheme.page(new BorderLayout(0, 18));
        JPanel top = AppTheme.transparent(new BorderLayout(0, 5));
        top.add(AppTheme.title("Submission review"), BorderLayout.NORTH);
        top.add(AppTheme.muted("Choose a student, then load answers to review and mark."), BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        JList<Submission> list = new JList<>();
        list.setFixedCellHeight(68);
        list.setBackground(AppTheme.SURFACE);
        list.setCellRenderer((items, value, index, selected, focus) -> {
            JPanel row = AppTheme.transparent(new GridLayout(2, 1, 0, 5));
            row.setOpaque(true);
            row.setBackground(selected ? AppTheme.SELECTION : AppTheme.SURFACE);
            row.setBorder(AppTheme.spacing(10, 14, 10, 14));
            row.add(AppTheme.section(value == null ? "" : "Student #" + value.getStudentId()));
            row.add(AppTheme.muted(value == null ? "" : "Submission #" + value.getId() + "  ·  "
                    + value.getStatus() + (value.getScore() == null ? "  ·  Awaiting grade"
                    : "  ·  " + value.getScore() + " marks")));
            return row;
        });
        JList<Answer> answers = new JList<>();
        answers.setFixedCellHeight(60);
        answers.setBackground(AppTheme.SURFACE);
        answers.setCellRenderer((items, value, index, selected, focus) -> {
            JPanel row = AppTheme.transparent(new GridLayout(2, 1, 0, 3));
            row.setOpaque(true);
            row.setBackground(selected ? AppTheme.SELECTION : AppTheme.SURFACE);
            row.setBorder(AppTheme.spacing(8, 14, 8, 14));
            row.add(AppTheme.section(value == null ? "" : "Question #" + value.getQuestionId()));
            String preview = value == null || value.getAnswerText() == null ? "No response"
                    : value.getAnswerText().replace('\n', ' ');
            row.add(AppTheme.muted(preview.length() > 70 ? preview.substring(0, 70) + "…" : preview));
            return row;
        });
        JPanel left = AppTheme.transparent(new BorderLayout(0, 8));
        left.add(AppTheme.section("Submissions"), BorderLayout.NORTH);
        left.add(AppTheme.scroll(list), BorderLayout.CENTER);
        JPanel right = AppTheme.transparent(new BorderLayout(0, 8));
        right.add(AppTheme.section("Selected student's answers"), BorderLayout.NORTH);
        right.add(AppTheme.scroll(answers), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setBorder(null);
        split.setOpaque(false);
        split.setResizeWeight(0.4);
        panel.add(split, BorderLayout.CENTER);
        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) answers.setListData(new Answer[0]); });

        JPanel actions = AppTheme.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        AppButton load = new AppButton("Load answers", AppButton.Kind.SECONDARY);
        AppButton mark = new AppButton("Mark selected answer", AppButton.Kind.SECONDARY);
        AppButton grade = new AppButton("Calculate grade", AppButton.Kind.PRIMARY);
        actions.add(load); actions.add(mark); actions.add(grade);
        panel.add(actions, BorderLayout.SOUTH);
        load.addActionListener(e -> {
            Submission submission = list.getSelectedValue();
            if (submission == null) { UiTasks.show(frame, "Select a submission first."); return; }
            UiTasks.run(frame, () -> context.submissions().reviewAnswers(teacher, submission.getId()),
                    results -> {
                        answers.setListData(results.toArray(Answer[]::new));
                        if (results.isEmpty()) UiTasks.show(frame, "No answers were recorded.");
                    });
        });
        answers.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() != 2 || answers.getSelectedValue() == null) return;
                JTextArea response = AppTheme.area(new JTextArea(answers.getSelectedValue().getAnswerText()));
                response.setEditable(false);
                JScrollPane scroll = AppTheme.scroll(response);
                scroll.setPreferredSize(new java.awt.Dimension(480, 230));
                JOptionPane.showMessageDialog(frame, scroll,
                        "Answer #" + answers.getSelectedValue().getId(), JOptionPane.PLAIN_MESSAGE);
            }
        });
        mark.addActionListener(e -> {
            Answer selectedAnswer = answers.getSelectedValue();
            String id = selectedAnswer == null ? JOptionPane.showInputDialog(frame, "Answer ID")
                    : Long.toString(selectedAnswer.getId());
            if (id == null) return;
            String points = JOptionPane.showInputDialog(frame,
                    selectedAnswer == null ? "Marks (manual only)" : "Marks for question #"
                            + selectedAnswer.getQuestionId() + " (manual only)");
            if (points == null) return;
            try {
                long answerId = Long.parseLong(id.trim());
                double score = Double.parseDouble(points.trim());
                UiTasks.run(frame, () -> { context.grading().recordManualMark(teacher, answerId, score); return null; },
                        ignored -> UiTasks.show(frame, "Manual mark saved. Recalculate the grade."));
            } catch (NumberFormatException ex) { UiTasks.show(frame, "Enter valid numbers for ID and marks."); }
        });
        grade.addActionListener(e -> {
            Submission submission = list.getSelectedValue();
            if (submission == null) { UiTasks.show(frame, "Select a submission first."); return; }
            UiTasks.run(frame, () -> context.grading().gradeForTeacher(teacher, submission.getId()),
                    result -> {
                        UiTasks.show(frame, result.requiresReview() ? "Still needs manual review or an MCQ key."
                                : "Grade calculated: " + result.score() + " marks.");
                        loadReviews(frame, list, exam.getId());
                    });
        });
        frame.setContentPane(panel);
        AppTheme.sizeWindow(frame, 890, 570, 620, 420);
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
        loadReviews(frame, list, exam.getId());
    }

    private void loadReviews(JFrame frame, JList<Submission> list, long examId) {
        UiTasks.run(frame, () -> context.submissions().listForReview(teacher, examId),
                results -> list.setListData(results.toArray(Submission[]::new)));
    }
}

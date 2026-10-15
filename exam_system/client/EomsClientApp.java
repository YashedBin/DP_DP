import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EomsClientApp extends JFrame {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient http = HttpClient.newHttpClient();

    // --- Color Palette ---
    private static final Color BG_DARK       = new Color(30, 30, 38);
    private static final Color BG_CARD       = new Color(42, 42, 54);
    private static final Color BG_INPUT      = new Color(55, 55, 70);
    private static final Color ACCENT        = new Color(99, 136, 250);
    private static final Color ACCENT_HOVER  = new Color(120, 155, 255);
    private static final Color SUCCESS       = new Color(80, 200, 120);
    private static final Color ERROR_RED     = new Color(240, 85, 85);
    private static final Color TEXT_PRIMARY   = new Color(230, 230, 240);
    private static final Color TEXT_SECONDARY = new Color(155, 155, 175);
    private static final Color DIVIDER       = new Color(60, 60, 78);
    private static final Color TEACHER_BADGE = new Color(88, 166, 255);
    private static final Color STUDENT_BADGE = new Color(255, 165, 50);

    // --- Fonts ---
    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_MONO    = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FONT_BTN     = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_Q_TITLE = new Font("Segoe UI", Font.BOLD, 13);
    private static final Color MCQ_TAG     = new Color(99, 136, 250);
    private static final Color SUBJ_TAG    = new Color(180, 130, 255);
    private static final Color CODE_TAG    = new Color(80, 200, 160);

    // --- Shared State ---
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private String currentExamId = "";
    private String loggedInName = "";
    private String loggedInRole = "";

    // synced exam labels across screens
    private final JLabel teacherExamLabel = makeLabel("", TEXT_SECONDARY, FONT_SMALL);
    private final JLabel studentExamLabel = makeLabel("", TEXT_SECONDARY, FONT_SMALL);
    private JTextField studentExamIdField;
    private JLabel headerUserLabel;

    public EomsClientApp() {
        setTitle("EOMS — Online Examination Management System");
        setSize(900, 680);
        setMinimumSize(new Dimension(780, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));
        root.add(buildLoginPanel(), "login");
        root.add(buildTeacherPanel(), "teacher");
        root.add(buildStudentPanel(), "student");
        add(root);
        cards.show(root, "login");
    }

    // ======================== LOGIN SCREEN ========================

    private JPanel buildLoginPanel() {
        JPanel outer = darkPanel(new GridBagLayout());

        JPanel card = darkCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 50, 40, 50));
        card.setMaximumSize(new Dimension(420, 500));
        card.setPreferredSize(new Dimension(420, 460));

        JLabel logo = makeLabel("EOMS", TEXT_PRIMARY, FONT_TITLE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = makeLabel("Your Digital Venue for Rigorous Certification", TEXT_SECONDARY, FONT_SMALL);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(32));

        // --- Teacher Login Section ---
        card.add(sectionHeader("Teacher Login", TEACHER_BADGE));
        card.add(Box.createVerticalStrut(8));

        JTextField teacherNameField = styledTextField("Enter username", 20);
        card.add(teacherNameField);
        card.add(Box.createVerticalStrut(10));

        JButton teacherBtn = accentButton("Continue as Teacher");
        teacherBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(teacherBtn);

        card.add(Box.createVerticalStrut(24));
        card.add(makeDivider());
        card.add(Box.createVerticalStrut(24));

        // --- Student Login Section ---
        card.add(sectionHeader("Student Login", STUDENT_BADGE));
        card.add(Box.createVerticalStrut(8));

        JTextField rollNoField = styledTextField("Enter Roll No (numeric ID)", 20);
        card.add(rollNoField);
        card.add(Box.createVerticalStrut(10));

        JButton studentBtn = accentButton("Continue as Student");
        studentBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(studentBtn);

        JLabel loginError = makeLabel(" ", ERROR_RED, FONT_SMALL);
        loginError.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(Box.createVerticalStrut(12));
        card.add(loginError);

        // --- Actions ---
        teacherBtn.addActionListener(e -> {
            String name = teacherNameField.getText().trim();
            if (name.isEmpty() || name.equals("Enter username")) {
                loginError.setText("Please enter a username.");
                return;
            }
            loggedInName = name;
            loggedInRole = "Teacher";
            updateHeaderLabel();
            cards.show(root, "teacher");
        });

        studentBtn.addActionListener(e -> {
            String roll = rollNoField.getText().trim();
            if (roll.isEmpty() || roll.equals("Enter Roll No (numeric ID)")) {
                loginError.setText("Please enter your Roll No.");
                return;
            }
            try {
                Integer.parseInt(roll);
            } catch (NumberFormatException ex) {
                loginError.setText("Roll No must be a number.");
                return;
            }
            loggedInName = roll;
            loggedInRole = "Student";
            updateHeaderLabel();
            cards.show(root, "student");
        });

        outer.add(card);
        return outer;
    }

    // ======================== TEACHER SCREEN ========================

    private JPanel buildTeacherPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 0));

        // --- Header Bar ---
        JPanel header = buildHeader("Teacher Dashboard", TEACHER_BADGE, teacherExamLabel);
        p.add(header, BorderLayout.NORTH);

        // --- Scrollable Content ---
        JPanel content = darkPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(16, 24, 16, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 14, 0);

        // ---- Section 1: Create Exam ----
        JPanel createCard = darkCard();
        createCard.setLayout(new BoxLayout(createCard, BoxLayout.Y_AXIS));
        createCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        createCard.add(cardTitle("Create Exam"));
        createCard.add(Box.createVerticalStrut(12));

        JTextField titleField = styledTextField("DBMS Midterm", 20);
        JTextField durationField = styledTextField("60", 6);
        JSpinner startSpinner = dateTimeSpinner();
        JSpinner endSpinner = dateTimeSpinner();
        // default end = 3 months from now
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 3);
        endSpinner.setValue(cal.getTime());

        createCard.add(fieldRow("Title", titleField));
        createCard.add(Box.createVerticalStrut(8));
        createCard.add(fieldRow("Duration (min)", durationField));
        createCard.add(Box.createVerticalStrut(8));
        createCard.add(fieldRow("Start Time", startSpinner));
        createCard.add(Box.createVerticalStrut(8));
        createCard.add(fieldRow("End Time", endSpinner));
        createCard.add(Box.createVerticalStrut(12));

        JButton createExamBtn = accentButton("Create Exam");
        JLabel createStatus = statusLabel();
        JPanel createBtnRow = leftRow(createExamBtn, createStatus);
        createCard.add(createBtnRow);

        gc.gridy = 0;
        content.add(createCard, gc);

        // ---- Section 2: Exam Control ----
        JPanel controlCard = darkCard();
        controlCard.setLayout(new BoxLayout(controlCard, BoxLayout.Y_AXIS));
        controlCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        controlCard.add(cardTitle("Exam Control"));
        controlCard.add(Box.createVerticalStrut(12));

        JTextField examIdField = styledTextField("", 8);
        controlCard.add(fieldRow("Exam ID", examIdField));
        controlCard.add(Box.createVerticalStrut(12));

        JButton loadExamBtn = accentButton("Load");
        JButton startExamBtn = accentButton("Start Exam");
        startExamBtn.setBackground(SUCCESS);
        JLabel controlStatus = statusLabel();

        JPanel ctrlBtnRow = leftRow(loadExamBtn, startExamBtn, controlStatus);
        controlCard.add(ctrlBtnRow);

        gc.gridy = 1;
        content.add(controlCard, gc);

        // ---- Section 3: Add Questions ----
        JPanel questionCard = darkCard();
        questionCard.setLayout(new BoxLayout(questionCard, BoxLayout.Y_AXIS));
        questionCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        questionCard.add(cardTitle("Add Questions"));
        questionCard.add(Box.createVerticalStrut(12));

        JComboBox<String> typeCombo = styledCombo(new String[]{"MCQ", "SUBJECTIVE", "CODING"});
        JTextField marksField = styledTextField("5", 4);
        JTextField qTextField = styledTextField("", 24);
        JTextField extraField = styledTextField("Options (MCQ) / Language (Coding)", 20);

        questionCard.add(fieldRow("Type", typeCombo));
        questionCard.add(Box.createVerticalStrut(8));
        questionCard.add(fieldRow("Marks", marksField));
        questionCard.add(Box.createVerticalStrut(8));
        questionCard.add(fieldRow("Question Text", qTextField));
        questionCard.add(Box.createVerticalStrut(8));
        questionCard.add(fieldRow("Extra", extraField));
        questionCard.add(Box.createVerticalStrut(12));

        JButton addQBtn = accentButton("Add Question");
        JLabel qStatus = statusLabel();
        questionCard.add(leftRow(addQBtn, qStatus));
        questionCard.add(Box.createVerticalStrut(12));

        DefaultListModel<String> qListModel = new DefaultListModel<>();
        JList<String> qList = styledList(qListModel);
        JScrollPane qScroll = styledScroll(qList, 140);
        questionCard.add(qScroll);

        gc.gridy = 2;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        content.add(questionCard, gc);

        JScrollPane mainScroll = new JScrollPane(content);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setBackground(BG_DARK);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        p.add(mainScroll, BorderLayout.CENTER);

        // --- Actions ---
        createExamBtn.addActionListener(e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            String startStr = sdf.format((Date) startSpinner.getValue());
            String endStr = sdf.format((Date) endSpinner.getValue());
            String json = "{\"title\":\"" + titleField.getText() + "\",\"durationMinutes\":" + durationField.getText()
                    + ",\"startTime\":\"" + startStr + "\",\"endTime\":\"" + endStr
                    + "\",\"status\":\"DRAFT\"}";
            String resp = post("/exams", json);
            String id = extractNumber(resp, "id");
            if (id != null) {
                setCurrentExam(id);
                examIdField.setText(id);
                setStatus(createStatus, "Exam #" + id + " created", true);
            } else {
                setStatus(createStatus, extractError(resp), false);
            }
        });

        loadExamBtn.addActionListener(e -> {
            String id = examIdField.getText().trim();
            if (id.isEmpty()) { setStatus(controlStatus, "Enter an exam ID", false); return; }
            String resp = get("/exams/" + id);
            String status = extractString(resp, "status");
            if (status != null) {
                setCurrentExam(id);
                setStatus(controlStatus, "Exam #" + id + " — " + status, true);
                refreshQuestionList(id, qListModel);
            } else {
                setStatus(controlStatus, extractError(resp), false);
            }
        });

        startExamBtn.addActionListener(e -> {
            String id = examIdField.getText().trim();
            if (id.isEmpty()) { setStatus(controlStatus, "Enter an exam ID", false); return; }
            String resp = post("/exams/" + id + "/start", "{}");
            String err = extractString(resp, "error");
            if (err != null) {
                setStatus(controlStatus, err, false);
            } else {
                setCurrentExam(id);
                setStatus(controlStatus, "Exam #" + id + " is now ONGOING", true);
            }
        });

        addQBtn.addActionListener(e -> {
            String id = examIdField.getText().trim();
            if (id.isEmpty()) { setStatus(qStatus, "Load or create an exam first", false); return; }
            String json = "{\"type\":\"" + typeCombo.getSelectedItem() + "\",\"text\":\"" + qTextField.getText()
                    + "\",\"marks\":" + marksField.getText() + ",\"extra\":\"" + extraField.getText() + "\"}";
            String resp = post("/exams/" + id + "/questions", json);
            String qId = extractNumber(resp, "id");
            if (qId != null) {
                setStatus(qStatus, "Question #" + qId + " added", true);
                qTextField.setText("");
                extraField.setText("");
                refreshQuestionList(id, qListModel);
            } else {
                setStatus(qStatus, extractError(resp), false);
            }
        });

        return p;
    }

    // ======================== STUDENT SCREEN ========================

    private JPanel buildStudentPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 0));

        JPanel header = buildHeader("Student Dashboard", STUDENT_BADGE, studentExamLabel);
        p.add(header, BorderLayout.NORTH);

        JPanel content = darkPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(16, 24, 16, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 14, 0);

        // ---- Section 1: View Questions ----
        JPanel viewCard = darkCard();
        viewCard.setLayout(new BoxLayout(viewCard, BoxLayout.Y_AXIS));
        viewCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        viewCard.add(cardTitle("View Questions"));
        viewCard.add(Box.createVerticalStrut(12));

        studentExamIdField = styledTextField(currentExamId, 8);
        viewCard.add(fieldRow("Exam ID", studentExamIdField));
        viewCard.add(Box.createVerticalStrut(12));

        JButton loadBtn = accentButton("Load Questions");
        JLabel loadStatus = statusLabel();
        viewCard.add(leftRow(loadBtn, loadStatus));
        viewCard.add(Box.createVerticalStrut(12));

        // Rendered question cards panel (scrollable)
        JPanel questionCardsPanel = new JPanel();
        questionCardsPanel.setLayout(new BoxLayout(questionCardsPanel, BoxLayout.Y_AXIS));
        questionCardsPanel.setBackground(BG_DARK);
        JScrollPane qCardsScroll = new JScrollPane(questionCardsPanel);
        qCardsScroll.setBorder(BorderFactory.createLineBorder(DIVIDER));
        qCardsScroll.getViewport().setBackground(BG_DARK);
        qCardsScroll.setPreferredSize(new Dimension(100, 320));
        qCardsScroll.getVerticalScrollBar().setUnitIncrement(16);
        qCardsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        viewCard.add(qCardsScroll);

        gc.gridy = 0;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        content.add(viewCard, gc);

        // ---- Section 2: Submit ----
        JPanel submitCard = darkCard();
        submitCard.setLayout(new BoxLayout(submitCard, BoxLayout.Y_AXIS));
        submitCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        submitCard.add(cardTitle("Submit Exam"));
        submitCard.add(Box.createVerticalStrut(12));

        JLabel rollInfo = makeLabel("Your Roll No: (login first)", TEXT_SECONDARY, FONT_SMALL);
        submitCard.add(rollInfo);
        submitCard.add(Box.createVerticalStrut(12));

        JButton submitBtn = accentButton("Submit Exam");
        submitBtn.setBackground(SUCCESS);
        JLabel submitStatus = statusLabel();
        submitCard.add(leftRow(submitBtn, submitStatus));

        gc.gridy = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        content.add(submitCard, gc);

        JScrollPane mainScroll = new JScrollPane(content);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setBackground(BG_DARK);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        p.add(mainScroll, BorderLayout.CENTER);

        // --- Update roll display when panel becomes visible ---
        p.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (loggedInRole.equals("Student")) {
                    rollInfo.setText("Your Roll No: " + loggedInName);
                }
            }
        });

        // --- Actions ---
        loadBtn.addActionListener(e -> {
            String id = studentExamIdField.getText().trim();
            if (id.isEmpty()) { setStatus(loadStatus, "Enter an exam ID", false); return; }
            setCurrentExam(id);
            buildQuestionCards(id, questionCardsPanel);
            setStatus(loadStatus, "Loaded questions for exam #" + id, true);
        });

        submitBtn.addActionListener(e -> {
            String id = studentExamIdField.getText().trim();
            if (id.isEmpty()) { setStatus(submitStatus, "Enter exam ID above", false); return; }
            if (!loggedInRole.equals("Student") || loggedInName.isEmpty()) {
                setStatus(submitStatus, "Login as Student first", false);
                return;
            }
            String json = "{\"studentId\":" + loggedInName + "}";
            String resp = post("/exams/" + id + "/submissions", json);
            String subId = extractNumber(resp, "id");
            if (subId != null) {
                setStatus(submitStatus, "Submitted! (submission #" + subId + ")", true);
            } else {
                setStatus(submitStatus, extractError(resp), false);
            }
        });

        return p;
    }

    // ======================== SHARED STATE ========================

    private void setCurrentExam(String id) {
        currentExamId = id;
        String display = id.isEmpty() ? "No exam loaded" : "Exam #" + id;
        teacherExamLabel.setText(display);
        studentExamLabel.setText(display);
        if (studentExamIdField != null) {
            studentExamIdField.setText(id);
        }
    }

    private void updateHeaderLabel() {
        // called after login, not used for a fixed label — role panels show their own header
    }

    private void refreshQuestionList(String examId, DefaultListModel<String> model) {
        model.clear();
        String resp = get("/exams/" + examId + "/questions");
        for (String block : splitJsonObjects(resp)) {
            String qId = extractNumber(block, "id");
            String type = extractString(block, "type");
            String text = extractString(block, "text");
            String marks = extractNumber(block, "marks");
            String opts = extractString(block, "options");
            if (qId != null) {
                String line = "#" + qId + "  [" + type + ", " + marks + " marks]  " + text;
                if (opts != null && !opts.isEmpty() && !opts.equals("null")) {
                    line += "  (" + opts + ")";
                }
                model.addElement(line);
            }
        }
        if (model.isEmpty()) {
            model.addElement("  No questions yet — exam might not be ONGOING");
        }
    }

    // ======================== UI COMPONENT BUILDERS ========================

    private JPanel buildHeader(String title, Color badge, JLabel examLabel) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton backBtn = new JButton("\u2190 Back");
        backBtn.setFont(FONT_BODY);
        backBtn.setForeground(TEXT_SECONDARY);
        backBtn.setBackground(BG_INPUT);
        backBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> cards.show(root, "login"));
        left.add(backBtn);

        JLabel titleLabel = makeLabel(title, TEXT_PRIMARY, FONT_HEADING);
        left.add(titleLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(examLabel);

        JLabel userBadge = makeLabel(
            loggedInRole.isEmpty() ? "" : loggedInRole + ": " + loggedInName,
            badge, FONT_SMALL
        );
        right.add(userBadge);

        // refresh user badge when the header's parent becomes visible
        header.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                userBadge.setText(loggedInRole.isEmpty() ? "" : loggedInRole + ": " + loggedInName);
            }
        });

        // also listen for ancestor events so it updates on card switch
        header.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                userBadge.setText(loggedInRole.isEmpty() ? "" : loggedInRole + ": " + loggedInName);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.add(header, BorderLayout.CENTER);

        JSeparator sep = new JSeparator();
        sep.setForeground(DIVIDER);
        sep.setBackground(DIVIDER);
        wrapper.add(sep, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel darkPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(BG_DARK);
        return p;
    }

    private JPanel darkCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JLabel makeLabel(String text, Color color, Font font) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(font);
        return l;
    }

    private JLabel cardTitle(String text) {
        JLabel l = makeLabel(text, TEXT_PRIMARY, FONT_HEADING);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel sectionHeader(String text, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel l = makeLabel(text, color, FONT_HEADING);
        p.add(l);
        return p;
    }

    private JTextField styledTextField(String placeholder, int cols) {
        JTextField tf = new JTextField(cols);
        tf.setFont(FONT_BODY);
        tf.setForeground(TEXT_PRIMARY);
        tf.setBackground(BG_INPUT);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tf.setAlignmentX(Component.CENTER_ALIGNMENT);

        // placeholder behavior
        if (!placeholder.isEmpty()) {
            tf.setText(placeholder);
            tf.setForeground(TEXT_SECONDARY);
            tf.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (tf.getText().equals(placeholder)) {
                        tf.setText("");
                        tf.setForeground(TEXT_PRIMARY);
                    }
                }
                @Override
                public void focusLost(FocusEvent e) {
                    if (tf.getText().isEmpty()) {
                        tf.setText(placeholder);
                        tf.setForeground(TEXT_SECONDARY);
                    }
                }
            });
        }

        return tf;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(FONT_BODY);
        cb.setForeground(TEXT_PRIMARY);
        cb.setBackground(BG_INPUT);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cb.setBorder(new EmptyBorder(2, 4, 2, 4));
        return cb;
    }

    private JButton accentButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? ACCENT_HOVER : getBackground();
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        b.setFont(FONT_BTN);
        b.setForeground(Color.WHITE);
        b.setBackground(ACCENT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setPreferredSize(new Dimension(160, 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    private JPanel fieldRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lbl = makeLabel(label, TEXT_SECONDARY, FONT_BODY);
        lbl.setPreferredSize(new Dimension(120, 28));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JPanel leftRow(JComponent... parts) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        for (JComponent c : parts) row.add(c);
        return row;
    }

    private JList<String> styledList(DefaultListModel<String> model) {
        JList<String> list = new JList<>(model);
        list.setFont(FONT_MONO);
        list.setBackground(BG_INPUT);
        list.setForeground(TEXT_PRIMARY);
        list.setSelectionBackground(ACCENT);
        list.setSelectionForeground(Color.WHITE);
        list.setBorder(new EmptyBorder(6, 8, 6, 8));
        return list;
    }

    private JScrollPane styledScroll(JComponent comp, int height) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setPreferredSize(new Dimension(100, height));
        sp.setBorder(BorderFactory.createLineBorder(DIVIDER));
        sp.getViewport().setBackground(BG_INPUT);
        sp.getVerticalScrollBar().setUnitIncrement(14);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sp;
    }

    private JSpinner dateTimeSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd  HH:mm");
        spinner.setEditor(editor);
        spinner.setFont(FONT_BODY);
        editor.getTextField().setFont(FONT_BODY);
        editor.getTextField().setForeground(TEXT_PRIMARY);
        editor.getTextField().setBackground(BG_INPUT);
        editor.getTextField().setCaretColor(TEXT_PRIMARY);
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        spinner.setPreferredSize(new Dimension(200, 36));
        return spinner;
    }

    private void buildQuestionCards(String examId, JPanel container) {
        container.removeAll();
        String resp = get("/exams/" + examId + "/questions");
        List<String> blocks = splitJsonObjects(resp);
        int qNum = 0;
        for (String block : blocks) {
            String qId = extractNumber(block, "id");
            String type = extractString(block, "type");
            String text = extractString(block, "text");
            String marks = extractNumber(block, "marks");
            String opts = extractString(block, "options");
            if (qId == null) continue;
            qNum++;

            JPanel card = darkCard();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(new EmptyBorder(14, 18, 14, 18));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));

            // Question header: Q number + type badge + marks
            JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            headerRow.setOpaque(false);
            headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel numLabel = makeLabel("Q" + qNum, TEXT_PRIMARY, FONT_Q_TITLE);
            headerRow.add(numLabel);

            Color tagColor = "MCQ".equals(type) ? MCQ_TAG : "CODING".equals(type) ? CODE_TAG : SUBJ_TAG;
            JLabel typeTag = makeLabel(" " + type + " ", tagColor, FONT_SMALL);
            typeTag.setOpaque(true);
            typeTag.setBackground(new Color(tagColor.getRed(), tagColor.getGreen(), tagColor.getBlue(), 40));
            typeTag.setBorder(new EmptyBorder(2, 6, 2, 6));
            headerRow.add(typeTag);

            JLabel marksLabel = makeLabel(marks + " marks", TEXT_SECONDARY, FONT_SMALL);
            headerRow.add(marksLabel);

            card.add(headerRow);
            card.add(Box.createVerticalStrut(8));

            // Question text
            JLabel qTextLabel = makeLabel(text, TEXT_PRIMARY, FONT_BODY);
            qTextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(qTextLabel);
            card.add(Box.createVerticalStrut(10));

            // Type-specific rendering
            if ("MCQ".equals(type) && opts != null && !opts.isEmpty() && !"null".equals(opts)) {
                ButtonGroup bg = new ButtonGroup();
                String[] options = opts.split(",");
                for (int i = 0; i < options.length; i++) {
                    JRadioButton rb = new JRadioButton((char)('A' + i) + ")  " + options[i].trim());
                    rb.setFont(FONT_BODY);
                    rb.setForeground(TEXT_PRIMARY);
                    rb.setBackground(BG_CARD);
                    rb.setFocusPainted(false);
                    rb.setOpaque(false);
                    rb.setAlignmentX(Component.LEFT_ALIGNMENT);
                    bg.add(rb);
                    card.add(rb);
                    card.add(Box.createVerticalStrut(4));
                }
            } else if ("SUBJECTIVE".equals(type)) {
                JTextArea answerArea = new JTextArea(3, 30);
                answerArea.setFont(FONT_BODY);
                answerArea.setForeground(TEXT_PRIMARY);
                answerArea.setBackground(BG_INPUT);
                answerArea.setCaretColor(TEXT_PRIMARY);
                answerArea.setLineWrap(true);
                answerArea.setWrapStyleWord(true);
                answerArea.setBorder(new EmptyBorder(8, 10, 8, 10));
                JScrollPane areaScroll = new JScrollPane(answerArea);
                areaScroll.setBorder(BorderFactory.createLineBorder(DIVIDER));
                areaScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
                areaScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                JLabel hint = makeLabel("Write your answer below:", TEXT_SECONDARY, FONT_SMALL);
                hint.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(hint);
                card.add(Box.createVerticalStrut(4));
                card.add(areaScroll);
            } else if ("CODING".equals(type)) {
                String lang = (opts != null && opts.startsWith("language:")) ? opts.substring(9) : "any";
                JLabel langLabel = makeLabel("Language: " + lang, CODE_TAG, FONT_SMALL);
                langLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(langLabel);
                card.add(Box.createVerticalStrut(4));
                JTextArea codeArea = new JTextArea(4, 30);
                codeArea.setFont(FONT_MONO);
                codeArea.setForeground(TEXT_PRIMARY);
                codeArea.setBackground(new Color(35, 35, 45));
                codeArea.setCaretColor(TEXT_PRIMARY);
                codeArea.setTabSize(4);
                codeArea.setBorder(new EmptyBorder(8, 10, 8, 10));
                JScrollPane codeScroll = new JScrollPane(codeArea);
                codeScroll.setBorder(BorderFactory.createLineBorder(DIVIDER));
                codeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
                codeScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
                card.add(codeScroll);
            }

            container.add(Box.createVerticalStrut(8));
            container.add(card);
        }

        if (qNum == 0) {
            JLabel emptyLabel = makeLabel("  No questions yet — exam might not be ONGOING", TEXT_SECONDARY, FONT_BODY);
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(Box.createVerticalStrut(16));
            container.add(emptyLabel);
        }

        container.revalidate();
        container.repaint();
    }

    private JSeparator makeDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(DIVIDER);
        sep.setBackground(DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        return sep;
    }

    private JLabel statusLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(FONT_SMALL);
        l.setForeground(TEXT_SECONDARY);
        return l;
    }

    private void setStatus(JLabel label, String text, boolean ok) {
        label.setText(text);
        label.setForeground(ok ? SUCCESS : ERROR_RED);
    }

    // ======================== HTTP + JSON HELPERS ========================

    private static String post(String path, String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (Exception ex) {
            return "{\"error\":\"Connection refused — is the server running?\"}";
        }
    }

    private static String get(String path) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (Exception ex) {
            return "{\"error\":\"Connection refused — is the server running?\"}";
        }
    }

    private static String extractNumber(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String extractString(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String extractError(String json) {
        String err = extractString(json, "error");
        if (err != null) return err;
        String msg = extractString(json, "message");
        if (msg != null) return msg;
        return "Something went wrong";
    }

    private static List<String> splitJsonObjects(String json) {
        List<String> blocks = new ArrayList<>();
        int depth = 0;
        int startIdx = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) startIdx = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && startIdx >= 0) {
                    blocks.add(json.substring(startIdx, i + 1));
                }
            }
        }
        return blocks;
    }

    // ======================== MAIN ========================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new EomsClientApp().setVisible(true));
    }
}

package ui;

import config.ApplicationContext;
import service.StudentSession;
import service.TeacherSession;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public final class LoginFrame extends JFrame {
    public LoginFrame(ApplicationContext context) {
        super("YDVRC — Sign in");
        AppTheme.install();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel page = AppTheme.page(new GridBagLayout());
        JPanel column = AppTheme.transparent(new BorderLayout(0, 28));
        JPanel identity = AppTheme.transparent(new BorderLayout(0, 5));
        identity.add(AppTheme.brand(), BorderLayout.NORTH);
        identity.add(AppTheme.muted("Your Digital Venue for Rigorous Certification"), BorderLayout.SOUTH);
        column.add(identity, BorderLayout.NORTH);

        JPanel card = AppTheme.surface(new BorderLayout(0, 20), 30);
        JPanel intro = AppTheme.transparent(new BorderLayout(0, 6));
        intro.add(AppTheme.title("Welcome back"), BorderLayout.NORTH);
        intro.add(AppTheme.muted("Sign in to continue to your examination space."), BorderLayout.SOUTH);
        card.add(intro, BorderLayout.NORTH);

        CardLayout views = new CardLayout();
        JPanel forms = AppTheme.transparent(views);
        JTextField teacherName = AppTheme.field(new JTextField());
        JPasswordField teacherPassword = AppTheme.field(new JPasswordField());
        AppButton teacherLogin = new AppButton("Sign in as teacher", AppButton.Kind.PRIMARY);
        JPanel teachers = form("Username", teacherName, "Password", teacherPassword, teacherLogin,
                "Teacher accounts are configured by an administrator.");

        JTextField studentId = AppTheme.field(new JTextField());
        JPasswordField studentPassword = AppTheme.field(new JPasswordField());
        AppButton studentLogin = new AppButton("Sign in as student", AppButton.Kind.PRIMARY);
        JPanel students = form("Student ID", studentId, "Password", studentPassword, studentLogin, null);
        forms.add(teachers, "teacher");
        forms.add(students, "student");

        JPanel middle = AppTheme.transparent(new BorderLayout(0, 20));
        JPanel roles = AppTheme.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        AppButton teacherTab = new AppButton("Teacher", AppButton.Kind.SECONDARY);
        AppButton studentTab = new AppButton("Student", AppButton.Kind.QUIET);
        roles.add(teacherTab);
        roles.add(studentTab);
        middle.add(roles, BorderLayout.NORTH);
        middle.add(forms, BorderLayout.CENTER);
        card.add(middle, BorderLayout.CENTER);

        JPanel foot = AppTheme.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        AppButton register = new AppButton("New student? Create an account", AppButton.Kind.QUIET);
        foot.add(register);
        register.setVisible(false);
        card.add(foot, BorderLayout.SOUTH);
        teacherTab.addActionListener(e -> {
            views.show(forms, "teacher");
            teacherTab.setEnabled(false);
            studentTab.setEnabled(true);
            register.setVisible(false);
        });
        studentTab.addActionListener(e -> {
            views.show(forms, "student");
            teacherTab.setEnabled(true);
            studentTab.setEnabled(false);
            register.setVisible(true);
        });
        teacherTab.setEnabled(false);

        column.add(card, BorderLayout.CENTER);
        GridBagConstraints place = new GridBagConstraints();
        place.fill = GridBagConstraints.HORIZONTAL;
        place.weightx = 1;
        place.gridx = 0;
        place.gridy = 0;
        place.ipadx = 12;
        page.add(column, place);

        teacherLogin.addActionListener(e -> {
            String username = teacherName.getText().trim();
            String password = new String(teacherPassword.getPassword());
            teacherPassword.setText("");
            UiTasks.run(this, () -> context.teachers().login(username, password),
                    (TeacherSession teacher) -> {
                        new TeacherDashboard(context, teacher).setVisible(true);
                        dispose();
                    });
        });
        studentLogin.addActionListener(e -> {
            long id;
            try { id = Long.parseLong(studentId.getText().trim()); }
            catch (NumberFormatException ex) { UiTasks.show(this, "Enter a numeric student ID"); return; }
            String password = new String(studentPassword.getPassword());
            studentPassword.setText("");
            UiTasks.run(this, () -> context.students().login(id, password),
                    (StudentSession student) -> {
                        new StudentDashboard(context, student).setVisible(true);
                        dispose();
                    });
        });
        register.addActionListener(e -> {
            JTextField name = AppTheme.field(new JTextField());
            JTextField email = AppTheme.field(new JTextField());
            JPasswordField password = AppTheme.field(new JPasswordField());
            JPanel fields = AppTheme.transparent(new java.awt.GridLayout(3, 2, 8, 8));
            fields.add(AppTheme.section("Name")); fields.add(name);
            fields.add(AppTheme.section("Email")); fields.add(email);
            fields.add(AppTheme.section("Password (8+ chars)")); fields.add(password);
            if (javax.swing.JOptionPane.showConfirmDialog(this, fields, "Create student account",
                    javax.swing.JOptionPane.OK_CANCEL_OPTION) != javax.swing.JOptionPane.OK_OPTION) return;
            String secret = new String(password.getPassword());
            String fullName = name.getText();
            String address = email.getText();
            password.setText("");
            UiTasks.run(this, () -> context.students().register(fullName, address, secret),
                    created -> UiTasks.show(this, "Your student ID is " + created.getId() + ". Keep it for sign-in."));
        });

        setContentPane(page);
        AppTheme.sizeWindow(this, 550, 580, 430, 490);
    }

    private static JPanel form(String firstName, JTextField first, String secondName,
            JPasswordField second, AppButton action, String note) {
        JPanel body = AppTheme.transparent(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        field(body, firstName, first);
        body.add(Box.createVerticalStrut(17));
        field(body, secondName, second);
        body.add(Box.createVerticalStrut(23));
        action.setAlignmentX(Component.LEFT_ALIGNMENT);
        action.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
        body.add(action);
        if (note != null) {
            body.add(Box.createVerticalStrut(14));
            JLabel hint = AppTheme.muted(note);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(hint);
        }
        return body;
    }

    private static void field(JPanel body, String name, JTextField input) {
        JLabel label = AppTheme.label(name, AppTheme.SMALL.deriveFont(java.awt.Font.BOLD), AppTheme.INK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
        body.add(label);
        body.add(Box.createVerticalStrut(7));
        body.add(input);
    }
}

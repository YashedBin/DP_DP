package com.eoms.ui;

import com.eoms.config.ApplicationContext;
import com.eoms.service.StudentSession;
import com.eoms.service.TeacherSession;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public final class LoginFrame extends JFrame {
    public LoginFrame(ApplicationContext context) {
        super("EOMS / YDVRC — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        root.add(new JLabel("Your Digital Venue for Rigorous Certification"), BorderLayout.NORTH);
        JPanel forms = new JPanel(new GridLayout(2, 1, 10, 15));

        JTextField teacherName = new JTextField();
        JPasswordField teacherPassword = new JPasswordField();
        JButton teacherLogin = new JButton("Teacher login");
        JPanel teachers = new JPanel(new GridLayout(3, 2, 8, 8));
        teachers.add(new JLabel("Teacher username")); teachers.add(teacherName);
        teachers.add(new JLabel("Password")); teachers.add(teacherPassword);
        teachers.add(teacherLogin); teachers.add(new JLabel("Provision teacher via SetupAccounts"));
        forms.add(teachers);

        JTextField studentId = new JTextField();
        JPasswordField studentPassword = new JPasswordField();
        JButton studentLogin = new JButton("Student login");
        JButton register = new JButton("Register new student");
        JPanel students = new JPanel(new GridLayout(3, 2, 8, 8));
        students.add(new JLabel("Student ID")); students.add(studentId);
        students.add(new JLabel("Password")); students.add(studentPassword);
        students.add(studentLogin); students.add(register);
        forms.add(students);
        root.add(forms, BorderLayout.CENTER);

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
            JTextField name = new JTextField();
            JTextField email = new JTextField();
            JPasswordField password = new JPasswordField();
            JPanel fields = new JPanel(new GridLayout(3, 2, 8, 8));
            fields.add(new JLabel("Name")); fields.add(name);
            fields.add(new JLabel("Email")); fields.add(email);
            fields.add(new JLabel("Password (8+ chars)")); fields.add(password);
            if (javax.swing.JOptionPane.showConfirmDialog(this, fields, "New student",
                    javax.swing.JOptionPane.OK_CANCEL_OPTION) != javax.swing.JOptionPane.OK_OPTION) return;
            String secret = new String(password.getPassword());
            String fullName = name.getText();
            String address = email.getText();
            password.setText("");
            UiTasks.run(this, () -> context.students().register(fullName, address, secret),
                    created -> UiTasks.show(this, "Registered. Your student ID is " + created.getId()));
        });

        setContentPane(root);
        setSize(610, 320);
        setLocationRelativeTo(null);
    }
}

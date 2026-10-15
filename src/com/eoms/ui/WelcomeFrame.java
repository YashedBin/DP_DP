package com.eoms.ui;

import com.eoms.model.Exam;
import com.eoms.service.ExamService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** Small, explicitly non-persistent preview while desktop workflows are built. */
public final class WelcomeFrame extends JFrame {
    public WelcomeFrame(ExamService examService) {
        super("EOMS / YDVRC — Desktop Preview");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(new EmptyBorder(20, 24, 20, 24));
        content.add(new JLabel("EOMS — Your Digital Venue for Rigorous Certification"), BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField title = new JTextField(22);
        JTextField duration = new JTextField("60", 6);
        fields.add(new JLabel("Exam title"));
        fields.add(title);
        fields.add(new JLabel("Duration (minutes)"));
        fields.add(duration);

        JButton preview = new JButton("Preview draft (not saved)");
        JLabel result = new JLabel("Database and login are not connected yet.");
        fields.add(preview);
        fields.add(result);
        content.add(fields, BorderLayout.CENTER);

        preview.addActionListener(event -> {
            try {
                int minutes = Integer.parseInt(duration.getText().trim());
                Exam exam = examService.prepareDraft(title.getText(), minutes, null);
                result.setText("Draft: " + exam.getTitle() + " (not saved)");
            } catch (IllegalArgumentException ex) {
                result.setText(ex.getMessage());
            }
        });

        setContentPane(content);
        pack();
        setLocationRelativeTo(null);
    }
}

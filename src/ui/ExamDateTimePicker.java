package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;

/** Date calendar and 24-hour time selector for the examination form. */
final class ExamDateTimePicker {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy");
    private LocalDate selectedDate;
    private final JButton dateButton = new AppButton("Choose date", AppButton.Kind.SECONDARY);
    private final JSpinner time;

    ExamDateTimePicker(LocalDateTime initial) {
        selectedDate = initial.toLocalDate();
        updateDateButton();
        dateButton.addActionListener(e -> openCalendar(dateButton));
        Date start = Date.from(initial.atZone(ZoneId.systemDefault()).toInstant());
        time = new JSpinner(new SpinnerDateModel(start, null, null, Calendar.MINUTE));
        time.setEditor(new JSpinner.DateEditor(time, "HH:mm"));
        time.setBackground(AppTheme.RAISED);
        time.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));
        AppTheme.field(((JSpinner.DateEditor) time.getEditor()).getTextField());
    }

    JButton dateButton() { return dateButton; }
    JSpinner timeSpinner() { return time; }

    LocalDateTime selected() throws ParseException {
        time.commitEdit();
        LocalTime chosenTime = ((Date) time.getValue()).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
        return LocalDateTime.of(selectedDate, chosenTime);
    }

    private void updateDateButton() { dateButton.setText(selectedDate.format(DATE)); }

    private void openCalendar(Component owner) {
        JDialog popup = new JDialog(SwingUtilities.getWindowAncestor(owner), "Choose a date",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content = AppTheme.surface(new BorderLayout(0, 16), 18);
        JPanel navigation = AppTheme.transparent(new BorderLayout(10, 0));
        JButton previous = new AppButton("←", AppButton.Kind.QUIET);
        JButton next = new AppButton("→", AppButton.Kind.QUIET);
        previous.setPreferredSize(new Dimension(42, 36));
        next.setPreferredSize(new Dimension(42, 36));
        JLabel monthLabel = AppTheme.section("");
        monthLabel.setHorizontalAlignment(JLabel.CENTER);
        navigation.add(previous, BorderLayout.WEST);
        navigation.add(monthLabel, BorderLayout.CENTER);
        navigation.add(next, BorderLayout.EAST);
        content.add(navigation, BorderLayout.NORTH);

        JPanel days = AppTheme.transparent(new GridLayout(0, 7, 4, 4));
        content.add(days, BorderLayout.CENTER);
        YearMonth[] viewed = {YearMonth.from(selectedDate)};
        Runnable refresh = () -> {
            days.removeAll();
            monthLabel.setText(viewed[0].format(MONTH));
            for (String weekday : new String[]{"M", "T", "W", "T", "F", "S", "S"}) {
                JLabel label = AppTheme.muted(weekday);
                label.setHorizontalAlignment(JLabel.CENTER);
                days.add(label);
            }
            int offset = viewed[0].atDay(1).getDayOfWeek().getValue() - 1;
            for (int blank = 0; blank < offset; blank++) days.add(AppTheme.transparent(new FlowLayout()));
            for (int day = 1; day <= viewed[0].lengthOfMonth(); day++) {
                LocalDate candidate = viewed[0].atDay(day);
                JButton choice = new AppButton(Integer.toString(day), candidate.equals(selectedDate)
                        ? AppButton.Kind.PRIMARY : AppButton.Kind.QUIET);
                choice.setPreferredSize(new Dimension(40, 36));
                choice.addActionListener(e -> {
                    selectedDate = candidate;
                    updateDateButton();
                    popup.dispose();
                });
                days.add(choice);
            }
            days.revalidate();
            days.repaint();
            popup.pack();
        };
        previous.addActionListener(e -> { viewed[0] = viewed[0].minusMonths(1); refresh.run(); });
        next.addActionListener(e -> { viewed[0] = viewed[0].plusMonths(1); refresh.run(); });
        popup.setContentPane(content);
        refresh.run();
        popup.setLocationRelativeTo(owner);
        popup.setVisible(true);
    }
}

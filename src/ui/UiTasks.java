package ui;

import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/** Runs service/JDBC work off the EDT; completion and messages return on the EDT. */
final class UiTasks {
    private UiTasks() {}

    static <T> void run(Component owner, Callable<T> work, Consumer<T> success) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                try {
                    success.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    show(owner, "Operation interrupted");
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    show(owner, cause.getMessage() == null ? cause.toString() : cause.getMessage());
                } catch (RuntimeException ex) {
                    show(owner, ex.getMessage() == null ? ex.toString() : ex.getMessage());
                }
            }
        }.execute();
    }

    static void show(Component owner, String text) {
        JOptionPane.showMessageDialog(owner, text, "EOMS", JOptionPane.INFORMATION_MESSAGE);
    }
}

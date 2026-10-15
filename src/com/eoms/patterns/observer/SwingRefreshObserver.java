package com.eoms.patterns.observer;

import java.sql.Connection;
import javax.swing.SwingUtilities;

/** Calls presentation callbacks only on the EDT, never on a JDBC worker thread. */
public final class SwingRefreshObserver implements ExamEventListener {
    private final Runnable refresh;
    public SwingRefreshObserver(Runnable refresh) { this.refresh = refresh; }
    @Override
    public void onEvent(ExamEvent event, Connection transaction) {
        SwingUtilities.invokeLater(refresh);
    }
}

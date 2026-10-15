package com.eoms.patterns.observer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ExamEventPublisher {
    private final List<ExamEventListener> listeners = new CopyOnWriteArrayList<>();
    private final List<ExamEventListener> postCommit = new CopyOnWriteArrayList<>();
    public void subscribe(ExamEventListener listener) { listeners.add(listener); }
    public void subscribeAfterCommit(ExamEventListener listener) { postCommit.add(listener); }
    public void unsubscribeAfterCommit(ExamEventListener listener) { postCommit.remove(listener); }
    public void publish(ExamEvent event, Connection transaction) throws SQLException {
        for (ExamEventListener listener : listeners) listener.onEvent(event, transaction);
    }
    public void afterCommit(ExamEvent event) {
        for (ExamEventListener listener : postCommit) {
            try { listener.onEvent(event, null); }
            catch (SQLException ex) { throw new IllegalStateException("Post-commit listener failed", ex); }
        }
    }
}

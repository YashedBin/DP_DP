package com.eoms;

import com.eoms.config.DatabaseConnection;
import com.eoms.patterns.bridge.ImmediateNotification;
import com.eoms.patterns.bridge.SwingChannel;
import com.eoms.patterns.observer.ExamEvent;
import com.eoms.patterns.observer.ExamEventPublisher;
import com.eoms.patterns.observer.SwingRefreshObserver;
import com.eoms.patterns.proxy.QuestionBankProxy;
import com.eoms.repository.ExamRepository;
import com.eoms.repository.StudentRepository;
import com.eoms.repository.SubmissionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

public final class CoordinationPatternsTest {
    public static void main(String[] args) throws Exception {
        DatabaseConnection unavailable = new DatabaseConnection(null, null, null);
        QuestionBankProxy proxy = new QuestionBankProxy(unavailable, new ExamRepository(unavailable),
                new StudentRepository(unavailable), new SubmissionRepository(unavailable),
                (id, student) -> { throw new AssertionError("Restricted questions accessed"); }, Clock.systemUTC());
        try { proxy.forStudent(1, null); throw new AssertionError("Unauthenticated access"); }
        catch (IllegalArgumentException expected) { /* rejected before any JDBC call */ }

        ExamEventPublisher events = new ExamEventPublisher();
        CountDownLatch refreshed = new CountDownLatch(1);
        AtomicBoolean onEdt = new AtomicBoolean();
        events.subscribeAfterCommit(new SwingRefreshObserver(() -> {
            onEdt.set(SwingUtilities.isEventDispatchThread());
            refreshed.countDown();
        }));
        events.afterCommit(new ExamEvent(ExamEvent.Type.EXAM_STARTED, 1, null, LocalDateTime.now()));
        assert refreshed.await(5, TimeUnit.SECONDS) && onEdt.get();

        CountDownLatch delivered = new CountDownLatch(1);
        new ImmediateNotification(new SwingChannel(text -> {
            onEdt.set(SwingUtilities.isEventDispatchThread());
            delivered.countDown();
        })).send("student", "Submitted");
        assert delivered.await(5, TimeUnit.SECONDS) && onEdt.get();
        System.out.println("Proxy denial, Observer dispatch and Bridge delivery checks passed.");
    }
}

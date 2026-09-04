package com.eoms.patterns.observer;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExamEventPublisher {

    private final List<ExamEventListener> listeners;

    public ExamEventPublisher(List<ExamEventListener> listeners) {
        this.listeners = listeners;
    }

    public void publish(ExamEvent event) {
        for (ExamEventListener listener : listeners) {
            listener.onExamEvent(event);
        }
    }
}

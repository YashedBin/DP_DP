package com.eoms.patterns.singleton;

import java.time.LocalDateTime;

public final class ExamClock {

    private static ExamClock instance;

    private ExamClock() {}

    public static synchronized ExamClock getInstance() {
        if (instance == null) {
            instance = new ExamClock();
        }
        return instance;
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}

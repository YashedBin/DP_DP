package com.eoms.config;

import com.eoms.service.DraftExamService;
import com.eoms.service.ExamService;
import com.eoms.ui.WelcomeFrame;
import java.time.Clock;

/** Explicit application wiring; no framework or global service locator. */
public final class ApplicationContext {
    private final ExamService examService;

    public ApplicationContext() {
        Clock clock = Clock.systemDefaultZone();
        examService = new DraftExamService(clock);
    }

    /** Called on the Swing event dispatch thread. */
    public WelcomeFrame createWelcomeFrame() {
        return new WelcomeFrame(examService);
    }
}

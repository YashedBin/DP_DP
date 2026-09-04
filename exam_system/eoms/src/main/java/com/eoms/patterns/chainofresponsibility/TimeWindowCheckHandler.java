package com.eoms.patterns.chainofresponsibility;

import com.eoms.patterns.singleton.ExamClock;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TimeWindowCheckHandler extends SubmissionHandler {

    @Override
    protected void check(SubmissionContext context) {
        LocalDateTime now = ExamClock.getInstance().now();
        if (now.isBefore(context.getExam().getStartTime()) || now.isAfter(context.getExam().getEndTime())) {
            context.reject("Submission is outside the exam's time window");
        }
    }
}

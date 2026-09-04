package com.eoms.patterns.chainofresponsibility;

import org.springframework.stereotype.Component;

@Component
public class AuthCheckHandler extends SubmissionHandler {

    @Override
    protected void check(SubmissionContext context) {
        if (context.getStudentId() == null) {
            context.reject("No student identity on submission");
        }
    }
}

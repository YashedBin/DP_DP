package com.eoms.patterns.chainofresponsibility;

import com.eoms.repository.SubmissionRepository;
import org.springframework.stereotype.Component;

@Component
public class AlreadySubmittedHandler extends SubmissionHandler {

    private final SubmissionRepository submissionRepository;

    public AlreadySubmittedHandler(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @Override
    protected void check(SubmissionContext context) {
        if (submissionRepository.existsByExamAndStudent(context.getExam().getId(), context.getStudentId())) {
            context.reject("Student has already submitted this exam");
        }
    }
}

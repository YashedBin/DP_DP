package com.eoms.service;

import com.eoms.entity.Exam;
import com.eoms.entity.Submission;
import com.eoms.patterns.bridge.ImmediateSender;
import com.eoms.patterns.chainofresponsibility.AlreadySubmittedHandler;
import com.eoms.patterns.chainofresponsibility.AuthCheckHandler;
import com.eoms.patterns.chainofresponsibility.SubmissionContext;
import com.eoms.patterns.chainofresponsibility.TimeWindowCheckHandler;
import com.eoms.patterns.observer.ExamEvent;
import com.eoms.patterns.observer.ExamEventPublisher;
import com.eoms.patterns.singleton.ExamClock;
import com.eoms.repository.ExamRepository;
import com.eoms.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ExamRepository examRepository;
    private final ExamEventPublisher eventPublisher;
    private final ImmediateSender notificationSender;
    private final AuthCheckHandler authCheckHandler;
    private final TimeWindowCheckHandler timeWindowCheckHandler;
    private final AlreadySubmittedHandler alreadySubmittedHandler;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            ExamRepository examRepository,
            ExamEventPublisher eventPublisher,
            ImmediateSender notificationSender,
            AuthCheckHandler authCheckHandler,
            TimeWindowCheckHandler timeWindowCheckHandler,
            AlreadySubmittedHandler alreadySubmittedHandler
    ) {
        this.submissionRepository = submissionRepository;
        this.examRepository = examRepository;
        this.eventPublisher = eventPublisher;
        this.notificationSender = notificationSender;
        this.authCheckHandler = authCheckHandler;
        this.timeWindowCheckHandler = timeWindowCheckHandler;
        this.alreadySubmittedHandler = alreadySubmittedHandler;

        authCheckHandler.setNext(timeWindowCheckHandler).setNext(alreadySubmittedHandler);
    }

    public Submission submit(Long examId, Long studentId) {
        Exam exam = examRepository.findById(examId);
        SubmissionContext context = new SubmissionContext(exam, studentId);

        authCheckHandler.handle(context);

        if (context.isRejected()) {
            throw new IllegalStateException(context.getRejectionReason());
        }

        Submission submission = new Submission(null, examId, studentId, ExamClock.getInstance().now(), "SUBMITTED", null);
        Submission saved = submissionRepository.save(submission);

        eventPublisher.publish(new ExamEvent(
                ExamEvent.Type.EXAM_SUBMITTED, examId, studentId, ExamClock.getInstance().now(), "Submission recorded"
        ));

        notificationSender.notify("student-" + studentId, "Your submission for exam " + examId + " was received.");

        return saved;
    }
}

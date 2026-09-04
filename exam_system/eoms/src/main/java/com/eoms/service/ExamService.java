package com.eoms.service;

import com.eoms.entity.Exam;
import com.eoms.patterns.observer.ExamEvent;
import com.eoms.patterns.observer.ExamEventPublisher;
import com.eoms.patterns.singleton.ExamClock;
import com.eoms.repository.ExamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamEventPublisher eventPublisher;

    public ExamService(ExamRepository examRepository, ExamEventPublisher eventPublisher) {
        this.examRepository = examRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public Exam getExam(Long id) {
        return examRepository.findById(id);
    }

    public Exam createExam(Exam exam) {
        exam.setStatus("DRAFT");
        return examRepository.save(exam);
    }

    public void startExam(Long id) {
        examRepository.updateStatus(id, "ONGOING");
        eventPublisher.publish(new ExamEvent(
                ExamEvent.Type.EXAM_STARTED, id, null, ExamClock.getInstance().now(), "Exam started"
        ));
    }
}

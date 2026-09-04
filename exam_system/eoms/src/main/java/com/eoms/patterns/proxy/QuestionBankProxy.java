package com.eoms.patterns.proxy;

import com.eoms.entity.Exam;
import com.eoms.entity.Question;
import com.eoms.repository.ExamRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class QuestionBankProxy implements QuestionBank {

    private final RealQuestionBank realQuestionBank;
    private final ExamRepository examRepository;

    public QuestionBankProxy(RealQuestionBank realQuestionBank, ExamRepository examRepository) {
        this.realQuestionBank = realQuestionBank;
        this.examRepository = examRepository;
    }

    @Override
    public List<Question> getQuestions(Long examId) {
        Exam exam = examRepository.findById(examId);
        if (exam == null || !"ONGOING".equals(exam.getStatus())) {
            return Collections.emptyList();
        }
        return realQuestionBank.getQuestions(examId);
    }
}

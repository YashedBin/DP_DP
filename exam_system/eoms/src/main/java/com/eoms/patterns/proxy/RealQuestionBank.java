package com.eoms.patterns.proxy;

import com.eoms.entity.Question;
import com.eoms.repository.QuestionRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RealQuestionBank implements QuestionBank {

    private final QuestionRepository questionRepository;

    public RealQuestionBank(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public List<Question> getQuestions(Long examId) {
        return questionRepository.findByExamId(examId);
    }
}

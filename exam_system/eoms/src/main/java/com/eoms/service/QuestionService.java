package com.eoms.service;

import com.eoms.entity.Question;
import com.eoms.patterns.factory.QuestionFactory;
import com.eoms.patterns.proxy.QuestionBankProxy;
import com.eoms.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QuestionService {

    private final Map<String, QuestionFactory> questionFactories;
    private final QuestionBankProxy questionBankProxy;
    private final QuestionRepository questionRepository;

    public QuestionService(
            Map<String, QuestionFactory> questionFactories,
            QuestionBankProxy questionBankProxy,
            QuestionRepository questionRepository
    ) {
        this.questionFactories = questionFactories;
        this.questionBankProxy = questionBankProxy;
        this.questionRepository = questionRepository;
    }

    public Question createQuestion(Long examId, String type, String text, int marks) {
        QuestionFactory factory = questionFactories.get(type.toUpperCase());
        if (factory == null) {
            throw new IllegalArgumentException("Unknown question type: " + type);
        }
        Question question = factory.createQuestion(examId, text, marks);
        return questionRepository.save(question);
    }

    public List<Question> getQuestions(Long examId) {
        return questionBankProxy.getQuestions(examId);
    }
}

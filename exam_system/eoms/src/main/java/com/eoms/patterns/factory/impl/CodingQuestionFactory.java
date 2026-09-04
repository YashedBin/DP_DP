package com.eoms.patterns.factory.impl;

import com.eoms.entity.Question;
import com.eoms.patterns.factory.QuestionFactory;
import org.springframework.stereotype.Component;

@Component("CODING")
public class CodingQuestionFactory implements QuestionFactory {

    @Override
    public Question createQuestion(Long examId, String text, int marks) {
        return new Question(null, examId, "CODING", text, marks);
    }
}

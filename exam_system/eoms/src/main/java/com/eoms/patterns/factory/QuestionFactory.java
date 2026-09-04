package com.eoms.patterns.factory;

import com.eoms.entity.Question;

public interface QuestionFactory {
    Question createQuestion(Long examId, String text, int marks);
}

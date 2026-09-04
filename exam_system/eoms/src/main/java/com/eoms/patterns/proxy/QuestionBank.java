package com.eoms.patterns.proxy;

import com.eoms.entity.Question;

import java.util.List;

public interface QuestionBank {
    List<Question> getQuestions(Long examId);
}

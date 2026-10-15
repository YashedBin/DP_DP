package com.eoms.patterns.proxy;

import com.eoms.model.Question;
import com.eoms.repository.QuestionRepository;
import com.eoms.service.StudentSession;
import java.sql.SQLException;
import java.util.List;

public final class RealQuestionBank implements QuestionBank {
    private final QuestionRepository questions;
    public RealQuestionBank(QuestionRepository questions) { this.questions = questions; }
    @Override
    public List<Question> forStudent(long examId, StudentSession student) throws SQLException {
        return questions.findByExamId(examId);
    }
}

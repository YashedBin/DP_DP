package com.eoms.patterns.proxy;

import com.eoms.model.Question;
import com.eoms.service.StudentSession;
import java.sql.SQLException;
import java.util.List;

public interface QuestionBank {
    List<Question> forStudent(long examId, StudentSession student) throws SQLException;
}

package patterns.proxy;

import model.Question;
import service.StudentSession;
import java.sql.SQLException;
import java.util.List;

public interface QuestionBank {
    List<Question> forStudent(long examId, StudentSession student) throws SQLException;
}

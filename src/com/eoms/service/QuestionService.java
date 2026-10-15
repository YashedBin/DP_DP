package com.eoms.service;

import com.eoms.model.Exam;
import com.eoms.model.Question;
import com.eoms.repository.ExamRepository;
import com.eoms.repository.QuestionRepository;
import com.eoms.patterns.proxy.QuestionBank;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** Editing operations only; student-facing access is deliberately deferred to the proxy. */
public final class QuestionService {
    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final QuestionBank studentQuestions;

    public QuestionService(ExamRepository exams, QuestionRepository questions) {
        this(exams, questions, null);
    }

    public QuestionService(ExamRepository exams, QuestionRepository questions, QuestionBank studentQuestions) {
        this.exams = exams;
        this.questions = questions;
        this.studentQuestions = studentQuestions;
    }

    public List<Question> listForStudent(long examId, StudentSession student) throws SQLException {
        if (studentQuestions == null) throw new IllegalStateException("Student question access is not configured");
        return studentQuestions.forStudent(examId, student);
    }

    public List<Question> listForEditor(long examId) throws SQLException {
        requireDraft(examId);
        return questions.findByExamId(examId);
    }

    public Question add(long examId, String type, String text, int marks, String options) throws SQLException {
        Question question = new Question(null, examId, normalizeType(type), text, marks, options);
        validate(question);
        requireDraft(examId);
        return questions.save(question);
    }

    public void update(Question question) throws SQLException {
        if (question == null || question.getId() == null || question.getExamId() == null) {
            throw new IllegalArgumentException("Question identity and exam are required");
        }
        requireDraft(question.getExamId());
        question.setType(normalizeType(question.getType()));
        validate(question);
        if (!questions.update(question)) throw new IllegalArgumentException("Question not found in exam");
    }

    public void remove(long examId, long questionId) throws SQLException {
        requireDraft(examId);
        if (!questions.delete(examId, questionId)) {
            throw new IllegalArgumentException("Question not found in exam");
        }
    }

    private void requireDraft(long examId) throws SQLException {
        Exam exam = exams.findById(examId);
        if (exam == null || !"DRAFT".equals(exam.getStatus())) {
            throw new IllegalStateException("Questions can only be edited in a draft exam");
        }
    }

    private static String normalizeType(String type) {
        if (type == null) throw new IllegalArgumentException("Question type is required");
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private static void validate(Question question) {
        if (!List.of("MCQ", "SUBJECTIVE", "CODING").contains(question.getType())) {
            throw new IllegalArgumentException("Question type must be MCQ, SUBJECTIVE or CODING");
        }
        if (question.getText() == null || question.getText().isBlank() || question.getMarks() <= 0) {
            throw new IllegalArgumentException("Question text and positive marks are required");
        }
        if ("MCQ".equals(question.getType()) &&
                (question.getOptions() == null || question.getOptions().isBlank())) {
            throw new IllegalArgumentException("MCQ options are required");
        }
    }
}

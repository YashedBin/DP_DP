package com.eoms.config;

import com.eoms.service.DraftExamService;
import com.eoms.service.ExamService;
import com.eoms.service.JdbcExamService;
import com.eoms.service.QuestionService;
import com.eoms.service.StudentService;
import com.eoms.service.SubmissionService;
import com.eoms.repository.AnswerRepository;
import com.eoms.repository.ExamRepository;
import com.eoms.repository.QuestionRepository;
import com.eoms.repository.StudentRepository;
import com.eoms.repository.SubmissionRepository;
import com.eoms.ui.WelcomeFrame;
import java.time.Clock;

/** Explicit application wiring; no framework or global service locator. */
public final class ApplicationContext {
    private final ExamService examService;
    private final QuestionService questionService;
    private final StudentService studentService;
    private final SubmissionService submissionService;

    public ApplicationContext() {
        Clock clock = Clock.systemDefaultZone();
        DatabaseConnection database = new DatabaseConnection();
        ExamRepository exams = new ExamRepository(database);
        QuestionRepository questions = new QuestionRepository(database);
        StudentRepository students = new StudentRepository(database);
        SubmissionRepository submissions = new SubmissionRepository(database);
        AnswerRepository answers = new AnswerRepository();
        examService = new JdbcExamService(new DraftExamService(clock), exams, questions, database, clock);
        questionService = new QuestionService(exams, questions);
        studentService = new StudentService(students);
        submissionService = new SubmissionService(database, exams, questions, students, submissions, answers, clock);
    }

    public ExamService exams() { return examService; }
    public QuestionService questions() { return questionService; }
    public StudentService students() { return studentService; }
    public SubmissionService submissions() { return submissionService; }

    /** Called on the Swing event dispatch thread. */
    public WelcomeFrame createWelcomeFrame() {
        return new WelcomeFrame(examService);
    }
}

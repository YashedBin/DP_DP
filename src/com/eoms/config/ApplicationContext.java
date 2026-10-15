package com.eoms.config;

import com.eoms.service.DraftExamService;
import com.eoms.service.ExamService;
import com.eoms.service.JdbcExamService;
import com.eoms.service.QuestionService;
import com.eoms.service.StudentService;
import com.eoms.service.SubmissionService;
import com.eoms.service.GradingService;
import com.eoms.service.TeacherService;
import com.eoms.patterns.bridge.DesktopChannel;
import com.eoms.patterns.bridge.ImmediateNotification;
import com.eoms.patterns.bridge.Notification;
import com.eoms.patterns.mediator.ExamLifecycleMediator;
import com.eoms.patterns.observer.AuditLogObserver;
import com.eoms.patterns.observer.ExamEventPublisher;
import com.eoms.patterns.proxy.QuestionBankProxy;
import com.eoms.patterns.proxy.RealQuestionBank;
import com.eoms.repository.AnswerRepository;
import com.eoms.repository.ExamRepository;
import com.eoms.repository.QuestionRepository;
import com.eoms.repository.StudentRepository;
import com.eoms.repository.SubmissionRepository;
import com.eoms.repository.TeacherRepository;
import com.eoms.ui.LoginFrame;
import java.time.Clock;

/** Explicit application wiring; no framework or global service locator. */
public final class ApplicationContext {
    private final ExamService examService;
    private final QuestionService questionService;
    private final StudentService studentService;
    private final SubmissionService submissionService;
    private final GradingService gradingService;
    private final TeacherService teacherService;
    private final ExamEventPublisher events;
    private final DesktopChannel desktopChannel;

    public ApplicationContext() {
        Clock clock = Clock.systemDefaultZone();
        DatabaseConnection database = new DatabaseConnection();
        ExamRepository exams = new ExamRepository(database);
        QuestionRepository questions = new QuestionRepository(database);
        StudentRepository students = new StudentRepository(database);
        SubmissionRepository submissions = new SubmissionRepository(database);
        AnswerRepository answers = new AnswerRepository();
        events = new ExamEventPublisher();
        events.subscribe(new AuditLogObserver());
        desktopChannel = new DesktopChannel();
        Notification notifications = new ImmediateNotification(desktopChannel);
        ExamLifecycleMediator lifecycle = new ExamLifecycleMediator(database, exams, events, notifications, clock);
        examService = new JdbcExamService(new DraftExamService(clock), exams, questions, database, clock, lifecycle);
        questionService = new QuestionService(exams, questions,
                new QuestionBankProxy(database, exams, students, submissions, new RealQuestionBank(questions), clock));
        studentService = new StudentService(students, submissions);
        teacherService = new TeacherService(new TeacherRepository(database));
        submissionService = new SubmissionService(database, exams, questions, students, submissions, answers,
                clock, events, notifications);
        gradingService = new GradingService(database);
    }

    public ExamService exams() { return examService; }
    public QuestionService questions() { return questionService; }
    public StudentService students() { return studentService; }
    public SubmissionService submissions() { return submissionService; }
    public GradingService grading() { return gradingService; }
    public TeacherService teachers() { return teacherService; }
    public ExamEventPublisher events() { return events; }
    public DesktopChannel notifications() { return desktopChannel; }

    /** Called on the Swing event dispatch thread. */
    public LoginFrame createLoginFrame() {
        return new LoginFrame(this);
    }
}

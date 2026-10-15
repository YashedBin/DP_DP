package config;

import database.DatabaseConnection;
import service.DraftExamService;
import service.ExamService;
import service.JdbcExamService;
import service.QuestionService;
import service.StudentService;
import service.SubmissionService;
import service.GradingService;
import service.TeacherService;
import patterns.bridge.DesktopChannel;
import patterns.bridge.ImmediateNotification;
import patterns.bridge.Notification;
import patterns.mediator.ExamLifecycleMediator;
import patterns.observer.AuditLogObserver;
import patterns.observer.ExamEventPublisher;
import patterns.proxy.QuestionBankProxy;
import patterns.proxy.RealQuestionBank;
import repository.AnswerRepository;
import repository.ExamRepository;
import repository.QuestionRepository;
import repository.StudentRepository;
import repository.SubmissionRepository;
import repository.TeacherRepository;
import ui.LoginFrame;
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
        this(new DatabaseConnection(), Clock.systemDefaultZone());
    }

    /** Explicit injection point for a dedicated test database and deterministic clock. */
    public ApplicationContext(DatabaseConnection database, Clock clock) {
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
        gradingService = new GradingService(database, events, clock);
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

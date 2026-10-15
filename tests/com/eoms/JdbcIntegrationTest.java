package com.eoms;

import config.ApplicationContext;
import database.DatabaseConnection;
import model.Exam;
import model.Question;
import model.Student;
import model.Submission;
import patterns.memento.DraftHistory;
import patterns.memento.ExamDraft;
import service.StudentSession;
import service.TeacherSession;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/** Opt-in integration check. Writes only to the explicitly supplied dedicated test DB. */
public final class JdbcIntegrationTest {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("EOMS_TEST_DB_URL");
        String user = System.getenv("EOMS_TEST_DB_USER");
        String password = System.getenv("EOMS_TEST_DB_PASSWORD");
        String teacherName = System.getenv("EOMS_TEST_TEACHER");
        String teacherPassword = System.getenv("EOMS_TEST_TEACHER_PASSWORD");
        if (url == null || user == null || password == null || teacherName == null || teacherPassword == null
                || !url.contains("eoms_test")) {
            throw new IllegalStateException("Requires EOMS_TEST_DB_* and EOMS_TEST_TEACHER* "
                    + "in a dedicated database whose URL contains eoms_test; see README");
        }
        ApplicationContext context = new ApplicationContext(new DatabaseConnection(url, user, password),
                Clock.systemDefaultZone());
        TeacherSession teacher = context.teachers().login(teacherName, teacherPassword);
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Student newStudent = context.students().register("Test " + unique, unique + "@example.test", "pass-" + unique);
        StudentSession student = context.students().login(newStudent.getId(), "pass-" + unique);

        Exam exam = context.exams().create(teacher, context.exams().prepareDraft("Test " + unique, 30, null));
        Question mcq = context.questions().add(teacher, exam.getId(), "MCQ", "2+2?", 5, "3,4");
        Question subjective = context.questions().add(teacher, exam.getId(), "SUBJECTIVE", "Explain", 3, null);
        context.grading().setMcqKey(teacher, mcq.getId(), "4");
        Exam copy = context.exams().duplicate(teacher, exam.getId());
        if (copy.getId().equals(exam.getId()) || context.questions().listForEditor(teacher, copy.getId()).size() != 2)
            throw new AssertionError("Exam duplication did not persist detached questions");
        ExamDraft editable = new ExamDraft(copy, context.questions().listForEditor(teacher, copy.getId()));
        var copied = context.questions().listForEditor(teacher, copy.getId());
        var keys = context.questions().answerKeysForEditor(teacher, copy.getId());
        editable.setAnswerKeys(copied.stream().map(q -> keys.get(q.getId())).collect(java.util.stream.Collectors.toList()));
        DraftHistory versions = new DraftHistory();
        versions.save("original", editable);
        editable.exam().setTitle("Changed");
        versions.restore("original", editable);
        context.exams().restoreDraft(teacher, copy.getId(), editable);
        if (context.questions().listForEditor(teacher, copy.getId()).size() != 2)
            throw new AssertionError("Draft restore lost questions");
        if (!context.questions().answerKeysForEditor(teacher, copy.getId()).containsValue("4"))
            throw new AssertionError("Draft restore lost MCQ answer key");
        context.exams().start(teacher, exam.getId());
        if (context.questions().listForStudent(exam.getId(), student).size() != 2)
            throw new AssertionError("Question proxy denied eligible student");
        Submission submitted = context.submissions().submit(exam.getId(), student,
                Map.of(mcq.getId(), "4", subjective.getId(), ""));
        try { context.questions().listForStudent(exam.getId(), student); throw new AssertionError("Questions leaked"); }
        catch (IllegalStateException expected) { /* proxy denied completed attempt */ }
        try { context.submissions().submit(exam.getId(), student, Map.of()); throw new AssertionError("Duplicate accepted"); }
        catch (IllegalStateException expected) { /* COR rejected */ }
        context.exams().end(teacher, exam.getId());
        var result = context.grading().gradeForTeacher(teacher, submitted.getId());
        if (result.requiresReview() || result.score() != 5.0) throw new AssertionError("Wrong score: " + result);
        context.grading().publish(teacher, exam.getId());
        if (context.students().publishedGrades(student).stream().noneMatch(s -> s.getId().equals(submitted.getId())))
            throw new AssertionError("Published grade not visible to student");
        System.out.println("JDBC exam, submission, grading and publication check passed (rows retained in test DB).");
    }
}

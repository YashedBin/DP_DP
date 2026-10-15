package com.eoms.service;

import com.eoms.model.Student;
import com.eoms.model.Submission;
import com.eoms.repository.StudentRepository;
import com.eoms.repository.SubmissionRepository;
import java.sql.SQLException;
import java.util.List;

/** Student records only; a row lookup is not proof of authentication. */
public final class StudentService {
    private final StudentRepository students;
    private final SubmissionRepository submissions;

    public StudentService(StudentRepository students, SubmissionRepository submissions) {
        this.students = students;
        this.submissions = submissions;
    }

    public Student findById(long id) throws SQLException {
        return students.findById(id);
    }

    public Student register(String name, String email, String password) throws SQLException {
        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Name and email are required");
        }
        return students.save(new Student(null, name.trim(), email.trim()), PasswordHashes.hash(password));
    }

    public StudentSession login(long studentId, String password) throws SQLException {
        if (!PasswordHashes.matches(password, students.passwordHash(studentId))) {
            throw new IllegalArgumentException("Invalid student credentials");
        }
        return new StudentSession(studentId);
    }

    public List<Submission> publishedGrades(StudentSession student) throws SQLException {
        if (student == null) throw new IllegalArgumentException("Student login required");
        return submissions.findPublishedForStudent(student.studentId());
    }
}

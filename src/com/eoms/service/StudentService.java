package com.eoms.service;

import com.eoms.model.Student;
import com.eoms.repository.StudentRepository;
import java.sql.SQLException;

/** Student records only; a row lookup is not proof of authentication. */
public final class StudentService {
    private final StudentRepository students;

    public StudentService(StudentRepository students) {
        this.students = students;
    }

    public Student findById(long id) throws SQLException {
        return students.findById(id);
    }

    public Student register(String name, String email) throws SQLException {
        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Name and email are required");
        }
        return students.save(new Student(null, name.trim(), email.trim()));
    }
}

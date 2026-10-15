package com.eoms.service;

import com.eoms.entity.Teacher;
import com.eoms.repository.TeacherRepository;
import org.springframework.stereotype.Service;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    public Teacher loginOrRegister(String username) {
        Teacher existing = teacherRepository.findByUsername(username);
        if (existing != null) {
            return existing;
        }
        return teacherRepository.save(new Teacher(null, username));
    }
}
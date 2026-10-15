package com.eoms.controller;

import com.eoms.entity.Teacher;
import com.eoms.service.TeacherService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/login")
    public Teacher login(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        return teacherService.loginOrRegister(username);
    }
}
package com.eoms.controller;

import com.eoms.entity.Exam;
import com.eoms.service.ExamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public List<Exam> getAllExams() {
        return examService.getAllExams();
    }

    @GetMapping("/{id}")
    public Exam getExam(@PathVariable Long id) {
        return examService.getExam(id);
    }

    @PostMapping
    public Exam createExam(@RequestBody Exam exam) {
        return examService.createExam(exam);
    }

    @PostMapping("/{id}/start")
    public void startExam(@PathVariable Long id) {
        examService.startExam(id);
    }
}

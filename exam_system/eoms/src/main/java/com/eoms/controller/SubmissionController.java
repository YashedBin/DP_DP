package com.eoms.controller;

import com.eoms.entity.Submission;
import com.eoms.service.SubmissionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exams/{examId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public Submission submit(@PathVariable Long examId, @RequestBody Map<String, Object> body) {
        Long studentId = Long.valueOf(body.get("studentId").toString());
        return submissionService.submit(examId, studentId);
    }
}

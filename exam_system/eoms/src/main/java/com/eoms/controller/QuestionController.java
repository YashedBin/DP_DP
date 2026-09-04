package com.eoms.controller;

import com.eoms.entity.Question;
import com.eoms.service.QuestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams/{examId}/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public List<Question> getQuestions(@PathVariable Long examId) {
        return questionService.getQuestions(examId);
    }

    @PostMapping
    public Question createQuestion(@PathVariable Long examId, @RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        String text = (String) body.get("text");
        int marks = ((Number) body.get("marks")).intValue();
        return questionService.createQuestion(examId, type, text, marks);
    }
}

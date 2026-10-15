package model;

import java.time.LocalDateTime;

public class Submission {
    private Long id;
    private Long examId;
    private Long studentId;
    private LocalDateTime submittedAt;
    private String status;
    private Double score;

    public Submission() {}

    public Submission(Long id, Long examId, Long studentId, LocalDateTime submittedAt, String status, Double score) {
        this.id = id;
        this.examId = examId;
        this.studentId = studentId;
        this.submittedAt = submittedAt;
        this.status = status;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}

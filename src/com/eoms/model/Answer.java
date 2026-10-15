package com.eoms.model;

public class Answer {
    private Long id;
    private Long submissionId;
    private Long questionId;
    private String answerText;

    public Answer() {}

    public Answer(Long id, Long submissionId, Long questionId, String answerText) {
        this.id = id;
        this.submissionId = submissionId;
        this.questionId = questionId;
        this.answerText = answerText;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}

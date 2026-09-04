package com.eoms.entity;

import java.time.LocalDateTime;

public class Exam {
    private Long id;
    private String title;
    private int durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public Exam() {}

    public Exam(Long id, String title, int durationMinutes, LocalDateTime startTime, LocalDateTime endTime, String status) {
        this.id = id;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

package com.focusbuddy.models;

import java.time.LocalDateTime;

public class PomodoroSession {
    private int id;
    private int goalId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public PomodoroSession() {}

    public PomodoroSession(int id, int goalId, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.goalId = goalId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGoalId() { return goalId; }
    public void setGoalId(int goalId) { this.goalId = goalId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}

package com.focusbuddy.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class Goal {
    public enum GoalType {
        STUDY_HOURS, TASKS_COMPLETED, FOCUS_SESSIONS
    }
    
    public enum Status {
        ACTIVE, COMPLETED, PAUSED
    }
    
    protected int id;
    protected int userId;
    protected String title;
    protected String description;
    protected int targetValue;
    protected int currentValue;
    protected GoalType goalType;
    protected LocalDate targetDate;
    protected Status status;
    protected LocalDateTime createdAt;
    protected String color; // Tambahkan field color
    
    public Goal() {
        this.status = Status.ACTIVE;
        this.currentValue = 0;
    }
    
    public abstract void updateProgress(int increment);
    public abstract double getProgressPercentage();
    public abstract boolean isCompleted();

    // Metode abstrak baru sesuai ERD
    public abstract void addTask(Task task);
    public abstract void addNote(Note note);
    public abstract void addPomodoroSession(PomodoroSession session);
    public abstract java.util.List<Task> getTasks();
    public abstract java.util.List<Note> getNotes();
    public abstract java.util.List<PomodoroSession> getPomodoroSessions();
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getTargetValue() { return targetValue; }
    public void setTargetValue(int targetValue) { this.targetValue = targetValue; }
    
    public int getCurrentValue() { return currentValue; }
    public void setCurrentValue(int currentValue) { this.currentValue = currentValue; }
    
    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }
    
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}

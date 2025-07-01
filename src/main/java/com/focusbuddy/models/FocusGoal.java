package com.focusbuddy.models;

import java.util.ArrayList;
import java.util.List;

public class FocusGoal extends Goal {

    private List<Task> tasks = new ArrayList<>();
    private List<Note> notes = new ArrayList<>();
    private List<PomodoroSession> pomodoroSessions = new ArrayList<>();

    public FocusGoal() {
        super();
        this.goalType = GoalType.FOCUS_SESSIONS;
    }

    public FocusGoal(String title, String description, int targetSessions) {
        this();
        this.title = title;
        this.description = description;
        this.targetValue = targetSessions;
    }

    @Override
    public void updateProgress(int sessionsIncrement) {
        this.currentValue += sessionsIncrement;
        if (isCompleted() && this.status == Status.ACTIVE) {
            this.status = Status.COMPLETED;
        }
    }

    @Override
    public double getProgressPercentage() {
        if (targetValue == 0) return 0.0;
        return Math.min(100.0, (double) currentValue / targetValue * 100.0);
    }

    @Override
    public boolean isCompleted() {
        return currentValue >= targetValue;
    }

    @Override
    public void addTask(Task task) {
        tasks.add(task);
    }

    @Override
    public void addNote(Note note) {
        notes.add(note);
    }

    @Override
    public void addPomodoroSession(PomodoroSession session) {
        pomodoroSessions.add(session);
    }

    @Override
    public List<Task> getTasks() {
        return tasks;
    }

    @Override
    public List<Note> getNotes() {
        return notes;
    }

    @Override
    public List<PomodoroSession> getPomodoroSessions() {
        return pomodoroSessions;
    }
}

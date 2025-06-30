package com.focusbuddy.services;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.BiConsumer;
import com.focusbuddy.observers.TimerObserver;
import java.util.ArrayList;
import java.util.List;

public class PomodoroTimer {
    private int focusDuration = 25 * 60; // 25 minutes in seconds
    private int breakDuration = 5 * 60;  // 5 minutes in seconds
    private int longBreakDuration = 20 * 60; // 20 minutes in seconds

    private Timeline timeline;
    private int currentSeconds;
    private int totalSeconds;
    private boolean isRunning;
    private boolean isFocusSession;

    private List<TimerObserver> observers = new ArrayList<>();

    private BiConsumer<Integer, Integer> onTimeUpdate;
    private Runnable onTimerComplete;

    public PomodoroTimer() {
        reset();
        setupTimeline();
    }

    private void setupTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (currentSeconds > 0) {
                currentSeconds--;
                updateDisplay();
            } else {
                complete();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void addObserver(TimerObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(TimerObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String event) {
        String timerType = isFocusSession ? "FOCUS" : "BREAK";
        switch (event) {
            case "START" -> observers.forEach(o -> o.onTimerStart(timerType));
            case "PAUSE" -> observers.forEach(o -> o.onTimerPause(timerType));
            case "RESET" -> observers.forEach(o -> o.onTimerReset(timerType));
            case "COMPLETE" -> observers.forEach(o -> o.onTimerComplete(timerType, totalSeconds - currentSeconds));
        }
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            timeline.play();
            notifyObservers("START");
        }
    }

    public void pause() {
        if (isRunning) {
            isRunning = false;
            timeline.pause();
            notifyObservers("PAUSE");
        }
    }

    public void reset() {
        if (timeline != null) {
            timeline.stop();
        }
        isRunning = false;
        isFocusSession = true;
        currentSeconds = focusDuration;
        totalSeconds = focusDuration;
        updateDisplay();
        notifyObservers("RESET");
    }

    private void complete() {
        timeline.stop();
        isRunning = false;

        notifyObservers("COMPLETE");

        if (onTimerComplete != null) {
            onTimerComplete.run();
        }

        // Switch between focus and break
        isFocusSession = !isFocusSession;
        if (isFocusSession) {
            currentSeconds = focusDuration;
            totalSeconds = focusDuration;
        } else {
            // You can add logic here to determine if it's a long break or short break
            currentSeconds = breakDuration;
            totalSeconds = breakDuration;
        }
        updateDisplay();
    }

    private void updateDisplay() {
        if (onTimeUpdate != null) {
            int minutes = currentSeconds / 60;
            int seconds = currentSeconds % 60;
            onTimeUpdate.accept(minutes, seconds);
        }
    }

    public double getProgress() {
        return 1.0 - ((double) currentSeconds / totalSeconds);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isFocusSession() {
        return isFocusSession;
    }

    // New setter methods for configurable durations
    public void setFocusDuration(int minutes) {
        this.focusDuration = minutes * 60;
        if (isFocusSession && !isRunning) {
            currentSeconds = focusDuration;
            totalSeconds = focusDuration;
            updateDisplay();
        }
    }

    public void setBreakDuration(int minutes) {
        this.breakDuration = minutes * 60;
        if (!isFocusSession && !isRunning) {
            currentSeconds = breakDuration;
            totalSeconds = breakDuration;
            updateDisplay();
        }
    }

    public void setLongBreakDuration(int minutes) {
        this.longBreakDuration = minutes * 60;
    }

    public int getFocusDurationMinutes() {
        return focusDuration / 60;
    }

    public int getBreakDurationMinutes() {
        return breakDuration / 60;
    }

    public int getLongBreakDurationMinutes() {
        return longBreakDuration / 60;
    }

    public void setOnTimeUpdate(BiConsumer<Integer, Integer> onTimeUpdate) {
        this.onTimeUpdate = onTimeUpdate;
    }

    public void setOnTimerComplete(Runnable onTimerComplete) {
        this.onTimerComplete = onTimerComplete;
    }

    // Method to set break duration (short or long)
    public void setCurrentBreakDuration(boolean isLongBreak) {
        if (!isFocusSession && !isRunning) {
            int duration = isLongBreak ? longBreakDuration : breakDuration;
            currentSeconds = duration;
            totalSeconds = duration;
            updateDisplay();
        }
    }
}
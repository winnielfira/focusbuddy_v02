package com.focusbuddy.controllers;

import com.focusbuddy.services.PomodoroTimer;
import com.focusbuddy.utils.NotificationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class PomodoroController {

    @FXML private Label sessionTypeLabel;
    @FXML private Label timerDisplay;
    @FXML private Button startTimerBtn;
    @FXML private Button pauseTimerBtn;
    @FXML private Button resetTimerBtn;
    @FXML private Button skipTimerBtn;
    @FXML private ProgressBar timerProgress;

    // Settings controls
    @FXML private Spinner<Integer> focusDurationSpinner;
    @FXML private Spinner<Integer> breakDurationSpinner;
    @FXML private Spinner<Integer> longBreakSpinner;
    @FXML private CheckBox autoStartBreakBox;
    @FXML private CheckBox soundNotificationBox;

    private PomodoroTimer pomodoroTimer;
    private int currentCycle = 1; // Track current pomodoro cycle

    @FXML
    private void initialize() {
        pomodoroTimer = new PomodoroTimer();

        setupSpinners();
        setupTimer();
        setupButtons();
        updateDisplay();
    }

    private void setupSpinners() {
        // Focus duration spinner (15-60 minutes)
        focusDurationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 60, 25)
        );

        // Break duration spinner (5-15 minutes)
        breakDurationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 15, 5)
        );

        // Long break spinner (15-30 minutes)
        longBreakSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 30, 20)
        );

        // Add listeners to update timer when values change
        focusDurationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!pomodoroTimer.isRunning()) {
                pomodoroTimer.getSettings().setFocusDuration(newVal);
                if (pomodoroTimer.isFocusSession()) {
                    updateTimerDisplay();
                }
            }
        });

        breakDurationSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!pomodoroTimer.isRunning()) {
                pomodoroTimer.getSettings().setBreakDuration(newVal);
                if (!pomodoroTimer.isFocusSession() && currentCycle % 4 != 0) {
                    updateTimerDisplay();
                }
            }
        });

        longBreakSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!pomodoroTimer.isRunning()) {
                pomodoroTimer.getSettings().setLongBreakDuration(newVal);
                if (!pomodoroTimer.isFocusSession() && currentCycle % 4 == 0) {
                    updateTimerDisplay();
                }
            }
        });
    }

    private void setupTimer() {
        // Set up timer display update
        pomodoroTimer.setOnTimeUpdate((minutes, seconds) -> {
            Platform.runLater(() -> {
                timerDisplay.setText(String.format("%02d:%02d", minutes, seconds));
                timerProgress.setProgress(pomodoroTimer.getProgress());
            });
        });

        // Set up timer completion
        pomodoroTimer.setOnTimerComplete(() -> {
            Platform.runLater(() -> {
                handleTimerComplete();
            });
        });

        // Initialize display
        updateTimerDisplay();
    }

    private void setupButtons() {
        startTimerBtn.setOnAction(e -> startTimer());
        pauseTimerBtn.setOnAction(e -> pauseTimer());
        resetTimerBtn.setOnAction(e -> resetTimer());
        skipTimerBtn.setOnAction(e -> skipSession());

        // Initially disable pause button
        pauseTimerBtn.setDisable(true);
    }

    private void startTimer() {
        pomodoroTimer.start();
        startTimerBtn.setDisable(true);
        pauseTimerBtn.setDisable(false);

        // Disable settings during session
        setSettingsEnabled(false);

        String sessionType = pomodoroTimer.isFocusSession() ? "Focus" : "Break";

        if (soundNotificationBox.isSelected()) {
            NotificationManager.getInstance().showNotification(
                    "Timer Started",
                    sessionType + " session started!",
                    NotificationManager.NotificationType.INFO
            );
        }
    }

    private void pauseTimer() {
        pomodoroTimer.pause();
        startTimerBtn.setDisable(false);
        pauseTimerBtn.setDisable(true);

        if (soundNotificationBox.isSelected()) {
            NotificationManager.getInstance().showNotification(
                    "Timer Paused",
                    "Session paused. Click start to resume.",
                    NotificationManager.NotificationType.INFO
            );
        }
    }

    private void resetTimer() {
        pomodoroTimer.reset();
        updateAfterReset();

        if (soundNotificationBox.isSelected()) {
            NotificationManager.getInstance().showNotification(
                    "Timer Reset",
                    "Timer has been reset.",
                    NotificationManager.NotificationType.INFO
            );
        }
    }

    private void skipSession() {
        if (pomodoroTimer.isRunning()) {
            pomodoroTimer.pause();
        }

        // Simulate timer completion
        handleTimerComplete();

        if (soundNotificationBox.isSelected()) {
            NotificationManager.getInstance().showNotification(
                    "Session Skipped",
                    "Session skipped to next phase.",
                    NotificationManager.NotificationType.INFO
            );
        }
    }

    private void handleTimerComplete() {
        boolean wasFocusSession = pomodoroTimer.isFocusSession();

        if (wasFocusSession) {
            // Focus session completed
            currentCycle++;

            // Show completion notification
            if (soundNotificationBox.isSelected()) {
                NotificationManager.getInstance().showNotification(
                        "Focus Session Complete!",
                        "Great job! Time for a " + getNextBreakType() + ".",
                        NotificationManager.NotificationType.SUCCESS
                );
            }

            // Auto-start break if enabled
            if (autoStartBreakBox.isSelected()) {
                Platform.runLater(() -> {
                    // Small delay before auto-starting
                    try {
                        Thread.sleep(1000);
                        if (!pomodoroTimer.isRunning()) {
                            startTimer();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        } else {
            // Break session completed
            if (soundNotificationBox.isSelected()) {
                NotificationManager.getInstance().showNotification(
                        "Break Complete!",
                        "Break time over. Ready for the next focus session?",
                        NotificationManager.NotificationType.INFO
                );
            }
        }

        updateAfterReset();
        updateSessionTypeLabel();
    }

    private String getNextBreakType() {
        return (currentCycle % 4 == 0) ? "long break" : "short break";
    }

    private void updateAfterReset() {
        startTimerBtn.setDisable(false);
        pauseTimerBtn.setDisable(true);
        setSettingsEnabled(true);
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        if (pomodoroTimer != null) {
            int currentDuration;
            if (pomodoroTimer.isFocusSession()) {
                currentDuration = focusDurationSpinner.getValue();
            } else {
                currentDuration = (currentCycle % 4 == 0) ?
                        longBreakSpinner.getValue() : breakDurationSpinner.getValue();
            }

            timerDisplay.setText(String.format("%02d:00", currentDuration));
            timerProgress.setProgress(0);
        }
    }

    private void updateSessionTypeLabel() {
        if (pomodoroTimer.isFocusSession()) {
            sessionTypeLabel.setText("Focus Session #" + (currentCycle + 1));
            sessionTypeLabel.setStyle("-fx-text-fill: #2196f3;");
        } else {
            String breakType = (currentCycle % 4 == 0) ? "Long Break" : "Short Break";
            sessionTypeLabel.setText(breakType);
            sessionTypeLabel.setStyle("-fx-text-fill: #4caf50;");
        }
    }

    private void setSettingsEnabled(boolean enabled) {
        focusDurationSpinner.setDisable(!enabled);
        breakDurationSpinner.setDisable(!enabled);
        longBreakSpinner.setDisable(!enabled);
    }

    private void updateDisplay() {
        updateSessionTypeLabel();
    }
}
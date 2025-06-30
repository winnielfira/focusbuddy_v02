package com.focusbuddy.controllers;

import com.focusbuddy.models.Task;
import com.focusbuddy.models.MoodEntry;
import com.focusbuddy.models.Goal;
import com.focusbuddy.services.PomodoroTimer;
import com.focusbuddy.services.TaskService;
import com.focusbuddy.services.MoodService;
import com.focusbuddy.services.GoalsService;
import com.focusbuddy.utils.ThemeManager;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.utils.NotificationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.chart.PieChart;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Arrays;
import java.util.Random;

public class DashboardController {

    @FXML private BorderPane dashboardContainer;
    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;
    @FXML private Label welcomeLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label greetingLabel;
    @FXML private Label dateLabel;
    @FXML private ToggleButton themeToggle;
    @FXML private Button logoutButton;

    // Navigation buttons
    @FXML private Button dashboardBtn;
    @FXML private Button tasksBtn;
    @FXML private Button pomodoroBtn;
    @FXML private Button moodBtn;
    @FXML private Button notesBtn;
    @FXML private Button goalsBtn;
    @FXML private Button profileBtn;

    // Stat cards
    @FXML private Label tasksCompletedLabel;
    @FXML private Label focusTimeLabel;
    @FXML private Label streakLabel;
    @FXML private Label goalsProgressLabel;

    // Quick timer
    @FXML private Label timerDisplay;
    @FXML private Button startTimerBtn;
    @FXML private Button pauseTimerBtn;
    @FXML private Button resetTimerBtn;
    @FXML private ProgressBar timerProgress;
    @FXML private Button fullPomodoroBtn;

    // Tasks section
    @FXML private VBox tasksList;
    @FXML private Button addTaskBtn;

    // Chart and activity
    @FXML private PieChart weeklyProgressChart;
    @FXML private VBox recentActivityList;

    // Quick actions
    @FXML private Button quickNoteBtn;
    @FXML private Button logMoodBtn;
    @FXML private Button createGoalBtn;
    @FXML private Button viewReportsBtn;

    // Motivation section
    @FXML private Label motivationQuote;
    @FXML private Button newTipBtn;

    private PomodoroTimer pomodoroTimer;
    private TaskService taskService;
    private MoodService moodService;
    private GoalsService goalsService;
    private String currentView = "dashboard";
    private Node dashboardContent;

    // Motivational quotes
    private final String[] motivationalQuotes = {
            "Focus on progress, not perfection. Every small step counts!",
            "The secret to getting ahead is getting started.",
            "Success is the sum of small efforts repeated day in and day out.",
            "Don't watch the clock; do what it does. Keep going.",
            "The future depends on what you do today.",
            "Productivity is never an accident. It's always the result of commitment.",
            "Focus on being productive instead of busy.",
            "Time management is life management.",
            "Small daily improvements lead to stunning results over time.",
            "The key to productivity is consistency, not perfection."
    };

    @FXML
    private void initialize() {
        // Initialize services
        taskService = new TaskService();
        moodService = new MoodService();
        goalsService = new GoalsService();
        pomodoroTimer = new PomodoroTimer();

        // Set welcome message
        if (UserSession.getInstance().isLoggedIn()) {
            welcomeLabel.setText("Welcome back, " +
                    UserSession.getInstance().getCurrentUser().getFullName() + "!");
        }

        // Store original dashboard content
        if (contentArea.getChildren().size() > 0) {
            dashboardContent = contentArea.getChildren().get(0);
        }

        // Set up all components
        setupNavigation();
        setupThemeToggle();
        setupTimer();
        setupQuickActions();
        setupTimeAndDate();
        loadDashboardData();
        setupResponsiveLayout();

        // Set up logout
        logoutButton.setOnAction(e -> handleLogout());

        // Start time update timer
        startTimeUpdater();
    }

    private void setupTimeAndDate() {
        updateTimeAndDate();
        updateGreeting();
    }

    private void updateTimeAndDate() {
        LocalDateTime now = LocalDateTime.now();

        // Update time
        currentTimeLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));

        // Update date
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
    }

    private void updateGreeting() {
        int hour = LocalDateTime.now().getHour();
        String greeting;

        if (hour < 12) {
            greeting = "Good Morning! ☀️";
        } else if (hour < 17) {
            greeting = "Good Afternoon! 🌤️";
        } else {
            greeting = "Good Evening! 🌙";
        }

        greetingLabel.setText(greeting);
    }

    private void startTimeUpdater() {
        Thread timeThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Update every minute
                    Platform.runLater(() -> {
                        updateTimeAndDate();
                        updateGreeting();
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timeThread.setDaemon(true);
        timeThread.start();
    }

    private void setupQuickActions() {
        quickNoteBtn.setOnAction(e -> showNotes());
        logMoodBtn.setOnAction(e -> showMoodTracker());
        createGoalBtn.setOnAction(e -> showGoals());
        viewReportsBtn.setOnAction(e -> {
            NotificationManager.getInstance().showNotification(
                    "Feature Coming Soon",
                    "Reports feature will be available in a future update!",
                    NotificationManager.NotificationType.INFO
            );
        });

        fullPomodoroBtn.setOnAction(e -> showPomodoro());
        addTaskBtn.setOnAction(e -> showTasks());
        newTipBtn.setOnAction(e -> updateMotivationalQuote());
    }

    private void updateMotivationalQuote() {
        Random random = new Random();
        String newQuote = motivationalQuotes[random.nextInt(motivationalQuotes.length)];
        motivationQuote.setText(newQuote);
    }

    private void loadDashboardData() {
        if (!UserSession.getInstance().isLoggedIn()) return;

        int userId = UserSession.getInstance().getCurrentUser().getId();

        try {
            // Load statistics
            loadStatistics(userId);

            // Load today's tasks
            loadTodaysTasks(userId);

            // Load weekly progress chart
            loadWeeklyProgress(userId);

            // Load recent activity
            loadRecentActivity(userId);

            // Set initial motivational quote
            updateMotivationalQuote();

        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.getInstance().showNotification(
                    "Error Loading Data",
                    "Some dashboard data could not be loaded.",
                    NotificationManager.NotificationType.WARNING
            );
        }
    }

    private void loadStatistics(int userId) {
        // Load tasks completed today
        List<Task> allTasks = taskService.getTasksForUser(userId);
        long tasksCompletedToday = allTasks.stream()
                .filter(task -> task.getStatus() == Task.Status.COMPLETED)
                .filter(task -> task.getCreatedAt() != null &&
                        task.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .count();
        tasksCompletedLabel.setText(String.valueOf(tasksCompletedToday));

        // Calculate focus time (simulated - in real app would come from focus sessions)
        int focusMinutes = (int) (tasksCompletedToday * 25); // Assume 25 min per completed task
        int hours = focusMinutes / 60;
        int minutes = focusMinutes % 60;
        focusTimeLabel.setText(String.format("%dh %dm", hours, minutes));

        // Calculate streak (simulated)
        int streak = calculateUserStreak(userId);
        streakLabel.setText(String.valueOf(streak));

        // Load goals progress
        List<Goal> goals = goalsService.getGoalsForUser(userId);
        long completedGoals = goals.stream()
                .filter(goal -> goal.getStatus() == Goal.Status.COMPLETED)
                .count();
        goalsProgressLabel.setText(String.format("%d/%d", completedGoals, goals.size()));
    }

    private int calculateUserStreak(int userId) {
        // This is a simplified calculation
        // In a real app, you'd track daily login/activity in the database
        List<MoodEntry> recentMoods = moodService.getRecentMoodEntries(userId, 7);
        return Math.min(recentMoods.size(), 7); // Simplified streak based on mood entries
    }

    private void loadTodaysTasks(int userId) {
        tasksList.getChildren().clear();

        List<Task> todayTasks = taskService.getTasksForUser(userId).stream()
                .filter(task -> task.getStatus() != Task.Status.COMPLETED)
                .limit(5) // Show only first 5 pending tasks
                .toList();

        if (todayTasks.isEmpty()) {
            Label noTasksLabel = new Label("🎉 No pending tasks! Great job!");
            noTasksLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-style: italic;");
            tasksList.getChildren().add(noTasksLabel);
        } else {
            for (Task task : todayTasks) {
                HBox taskItem = createTaskItem(task);
                tasksList.getChildren().add(taskItem);
            }
        }
    }

    private HBox createTaskItem(Task task) {
        HBox taskItem = new HBox(10);
        taskItem.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8; -fx-background-radius: 5;");

        // Priority indicator
        Label priorityDot = new Label("●");
        switch (task.getPriority()) {
            case HIGH -> priorityDot.setStyle("-fx-text-fill: #F44336; -fx-font-size: 16px;");
            case MEDIUM -> priorityDot.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 16px;");
            case LOW -> priorityDot.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 16px;");
        }

        // Task content
        VBox taskContent = new VBox(2);
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label statusLabel = new Label(task.getStatus().toString());
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        taskContent.getChildren().addAll(titleLabel, statusLabel);

        // Due date (if exists)
        Label dueDateLabel = new Label();
        if (task.getDueDate() != null) {
            dueDateLabel.setText(task.getDueDate().format(DateTimeFormatter.ofPattern("MMM d")));
            dueDateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        }

        taskItem.getChildren().addAll(priorityDot, taskContent, dueDateLabel);
        HBox.setHgrow(taskContent, Priority.ALWAYS);

        return taskItem;
    }

    private void loadWeeklyProgress(int userId) {
        try {
            List<Task> allTasks = taskService.getTasksForUser(userId);
            List<Goal> allGoals = goalsService.getGoalsForUser(userId);
            List<MoodEntry> weekMoods = moodService.getRecentMoodEntries(userId, 7);

            // Create pie chart data
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Completed Tasks", allTasks.stream()
                            .filter(t -> t.getStatus() == Task.Status.COMPLETED).count()),
                    new PieChart.Data("Pending Tasks", allTasks.stream()
                            .filter(t -> t.getStatus() == Task.Status.PENDING).count()),
                    new PieChart.Data("Active Goals", allGoals.stream()
                            .filter(g -> g.getStatus() == Goal.Status.ACTIVE).count()),
                    new PieChart.Data("Mood Entries", weekMoods.size())
            );

            weeklyProgressChart.setData(pieChartData);
            weeklyProgressChart.setLegendVisible(false);
            weeklyProgressChart.setLabelsVisible(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentActivity(int userId) {
        recentActivityList.getChildren().clear();

        try {
            // Get recent tasks
            List<Task> recentTasks = taskService.getTasksForUser(userId).stream()
                    .filter(task -> task.getStatus() == Task.Status.COMPLETED)
                    .limit(3)
                    .toList();

            // Get recent goals
            List<Goal> recentGoals = goalsService.getGoalsForUser(userId).stream()
                    .limit(2)
                    .toList();

            // Add activities
            for (Task task : recentTasks) {
                Label activityLabel = new Label("✅ Completed task: " + task.getTitle());
                activityLabel.setStyle("-fx-font-size: 12px; -fx-padding: 3;");
                recentActivityList.getChildren().add(activityLabel);
            }

            for (Goal goal : recentGoals) {
                String status = goal.getStatus() == Goal.Status.COMPLETED ? "🎯 Completed goal:" : "📝 Working on goal:";
                Label activityLabel = new Label(status + " " + goal.getTitle());
                activityLabel.setStyle("-fx-font-size: 12px; -fx-padding: 3;");
                recentActivityList.getChildren().add(activityLabel);
            }

            // Add some simulated focus sessions
            recentActivityList.getChildren().add(new Label("🍅 Completed 25-min focus session"));
            recentActivityList.getChildren().add(new Label("😊 Logged daily mood"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTimer() {
        if (timerDisplay == null) return;

        // Set up timer display update
        pomodoroTimer.setOnTimeUpdate((minutes, seconds) -> {
            Platform.runLater(() -> {
                if (timerDisplay != null) {
                    timerDisplay.setText(String.format("%02d:%02d", minutes, seconds));
                }
                if (timerProgress != null) {
                    double progress = pomodoroTimer.getProgress();
                    timerProgress.setProgress(progress);
                }
            });
        });

        // Set up timer completion
        pomodoroTimer.setOnTimerComplete(() -> {
            Platform.runLater(() -> {
                NotificationManager.getInstance().showNotification(
                        "Focus Session Complete!",
                        "Great job! Time for a break.",
                        NotificationManager.NotificationType.SUCCESS
                );
                if (resetTimerBtn != null) {
                    resetTimerBtn.fire();
                }
                // Refresh stats after completion
                loadDashboardData();
            });
        });

        // Set up timer controls
        if (startTimerBtn != null) {
            startTimerBtn.setOnAction(e -> {
                pomodoroTimer.start();
                startTimerBtn.setDisable(true);
                pauseTimerBtn.setDisable(false);
            });
        }
        if (pauseTimerBtn != null) {
            pauseTimerBtn.setOnAction(e -> {
                pomodoroTimer.pause();
                startTimerBtn.setDisable(false);
                pauseTimerBtn.setDisable(true);
            });
        }
        if (resetTimerBtn != null) {
            resetTimerBtn.setOnAction(e -> {
                pomodoroTimer.reset();
                startTimerBtn.setDisable(false);
                pauseTimerBtn.setDisable(true);
                if (timerDisplay != null) {
                    timerDisplay.setText("25:00");
                }
                if (timerProgress != null) {
                    timerProgress.setProgress(0);
                }
            });
        }

        // Initialize display
        if (timerDisplay != null) {
            timerDisplay.setText("25:00");
        }
        if (timerProgress != null) {
            timerProgress.setProgress(0);
        }
        if (pauseTimerBtn != null) {
            pauseTimerBtn.setDisable(true);
        }
    }

    private void setupResponsiveLayout() {
        Platform.runLater(() -> {
            Stage stage = (Stage) dashboardContainer.getScene().getWindow();
            if (stage != null) {
                stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                    adjustLayoutForSize(newWidth.doubleValue(), stage.getHeight());
                });

                stage.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                    adjustLayoutForSize(stage.getWidth(), newHeight.doubleValue());
                });
            }
        });
    }

    private void adjustLayoutForSize(double width, double height) {
        if (width < 1200) {
            sidebar.setPrefWidth(200);
        } else {
            sidebar.setPrefWidth(250);
        }

        if (width < 1000) {
            contentArea.setStyle("-fx-padding: 10;");
        } else {
            contentArea.setStyle("-fx-padding: 20;");
        }
    }

    private void setupNavigation() {
        dashboardBtn.setOnAction(e -> showDashboard());
        tasksBtn.setOnAction(e -> showTasks());
        pomodoroBtn.setOnAction(e -> showPomodoro());
        moodBtn.setOnAction(e -> showMoodTracker());
        notesBtn.setOnAction(e -> showNotes());
        goalsBtn.setOnAction(e -> showGoals());
        profileBtn.setOnAction(e -> showProfile());

        setActiveButton(dashboardBtn);
    }

    private void showProfile() {
        setActiveButton(profileBtn);
        currentView = "profile";
        loadView("/fxml/profile.fxml");
    }

    private void setupThemeToggle() {
        if (ThemeManager.getInstance().getCurrentTheme() == ThemeManager.Theme.DARK) {
            themeToggle.setText("🌙");
            themeToggle.setSelected(true);
        } else {
            themeToggle.setText("☀️");
            themeToggle.setSelected(false);
        }

        themeToggle.setOnAction(e -> {
            ThemeManager.getInstance().toggleTheme(dashboardContainer.getScene());
            if (ThemeManager.getInstance().getCurrentTheme() == ThemeManager.Theme.DARK) {
                themeToggle.setText("🌙");
            } else {
                themeToggle.setText("☀️");
            }
        });
    }

    private void setActiveButton(Button activeBtn) {
        dashboardBtn.getStyleClass().remove("active");
        tasksBtn.getStyleClass().remove("active");
        pomodoroBtn.getStyleClass().remove("active");
        moodBtn.getStyleClass().remove("active");
        notesBtn.getStyleClass().remove("active");
        goalsBtn.getStyleClass().remove("active");
        profileBtn.getStyleClass().remove("active");

        activeBtn.getStyleClass().add("active");
    }

    private void showDashboard() {
        if (!currentView.equals("dashboard")) {
            setActiveButton(dashboardBtn);
            currentView = "dashboard";

            contentArea.getChildren().clear();
            if (dashboardContent != null) {
                contentArea.getChildren().add(dashboardContent);
            }

            setupTimer();
            loadDashboardData();
        }
    }

    private void showTasks() {
        setActiveButton(tasksBtn);
        currentView = "tasks";
        loadView("/fxml/tasks.fxml");
    }

    private void showPomodoro() {
        setActiveButton(pomodoroBtn);
        currentView = "pomodoro";
        loadView("/fxml/pomodoro.fxml");
    }

    private void showMoodTracker() {
        setActiveButton(moodBtn);
        currentView = "mood";
        loadView("/fxml/mood-tracker.fxml");
    }

    private void showNotes() {
        setActiveButton(notesBtn);
        currentView = "notes";
        loadView("/fxml/notes.fxml");
    }

    private void showGoals() {
        setActiveButton(goalsBtn);
        currentView = "goals";
        loadView("/fxml/goals.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.getInstance().showNotification(
                    "Error",
                    "Failed to load view: " + e.getMessage(),
                    NotificationManager.NotificationType.ERROR
            );
        }
    }

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("Any unsaved changes will be lost.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                UserSession.getInstance().logout();
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));

            Stage stage = (Stage) dashboardContainer.getScene().getWindow();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            Scene scene = new Scene(loader.load());
            ThemeManager.getInstance().applyTheme(scene, ThemeManager.getInstance().getCurrentTheme());

            stage.setScene(scene);
            stage.setTitle("FocusBuddy - Login");

            stage.setMaximized(false);
            stage.setWidth(800);
            stage.setHeight(600);
            stage.setResizable(true);

            stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
            stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
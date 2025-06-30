package com.focusbuddy.controllers;

import com.focusbuddy.models.Task;
import com.focusbuddy.services.TaskService;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class TasksController {

    @FXML private VBox tasksContainer;
    @FXML private ListView<Task> tasksList;
    @FXML private TextField taskTitleField;
    @FXML private TextArea taskDescriptionArea;
    @FXML private ComboBox<Task.Priority> priorityCombo;
    @FXML private ComboBox<Task.Status> statusCombo;
    @FXML private DatePicker dueDatePicker;
    @FXML private Button saveTaskButton;
    @FXML private Button newTaskButton;
    @FXML private Button deleteTaskButton;

    // Filtering and sorting controls
    @FXML private TextField searchField;
    @FXML private ComboBox<Task.Priority> filterPriorityCombo;
    @FXML private ComboBox<Task.Status> filterStatusCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private CheckBox showCompletedCheckBox;

    // Statistics
    @FXML private Label totalTasksLabel;
    @FXML private Label pendingTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label overdueTasksLabel;

    private TaskService taskService;
    private Task currentTask;
    private ObservableList<Task> allTasks;

    @FXML
    private void initialize() {
        taskService = new TaskService();
        allTasks = FXCollections.observableArrayList();

        setupTasksList();
        setupComboBoxes();
        setupButtons();
        setupFilters();
        loadTasks();
        updateStatistics();
    }

    private void setupTasksList() {
        tasksList.setCellFactory(listView -> new ListCell<Task>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(""); // Reset style
                } else {
                    VBox content = new VBox(3);

                    Label titleLabel = new Label(task.getTitle());
                    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                    Label descLabel = new Label(task.getDescription());
                    descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
                    descLabel.setWrapText(true);

                    Label priorityLabel = new Label("Priority: " + task.getPriority());
                    priorityLabel.setStyle(getPriorityStyle(task.getPriority()));

                    Label statusLabel = new Label("Status: " + task.getStatus());
                    statusLabel.setStyle(getStatusStyle(task.getStatus()));

                    if (task.getDueDate() != null) {
                        Label dueDateLabel = new Label("Due: " + task.getDueDate());
                        if (task.getDueDate().isBefore(LocalDate.now()) && task.getStatus() != Task.Status.COMPLETED) {
                            dueDateLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        }
                        content.getChildren().add(dueDateLabel);
                    }

                    content.getChildren().addAll(titleLabel, descLabel, priorityLabel, statusLabel);
                    setGraphic(content);

                    // Set background color based on priority and selection
                    String baseStyle = getTaskItemStyle(task);

                    // Add selection highlighting
                    if (isSelected()) {
                        setStyle(baseStyle + " -fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-width: 2px;");
                    } else {
                        setStyle(baseStyle);
                    }
                }
            }
        });

        // Handle selection changes
        tasksList.getSelectionModel().selectedItemProperty().addListener((obs, oldTask, newTask) -> {
            if (newTask != null) {
                loadTaskDetails(newTask);
                // Enable delete button when something is selected
                deleteTaskButton.setDisable(false);
                deleteTaskButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            } else {
                // Disable delete button when nothing is selected
                deleteTaskButton.setDisable(true);
                deleteTaskButton.setStyle("");
            }
        });

        // Allow multiple selection for better UX
        tasksList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private String getPriorityStyle(Task.Priority priority) {
        return switch (priority) {
            case HIGH -> "-fx-text-fill: #F44336; -fx-font-weight: bold;";
            case MEDIUM -> "-fx-text-fill: #FF9800; -fx-font-weight: bold;";
            case LOW -> "-fx-text-fill: #4CAF50; -fx-font-weight: bold;";
        };
    }

    private String getStatusStyle(Task.Status status) {
        return switch (status) {
            case COMPLETED -> "-fx-text-fill: #4CAF50; -fx-font-weight: bold;";
            case IN_PROGRESS -> "-fx-text-fill: #2196F3; -fx-font-weight: bold;";
            case PENDING -> "-fx-text-fill: #FF9800; -fx-font-weight: bold;";
        };
    }

    private String getTaskItemStyle(Task task) {
        if (task.getStatus() == Task.Status.COMPLETED) {
            return "-fx-background-color: #E8F5E8; -fx-background-radius: 5px; -fx-padding: 8px;";
        } else if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            return "-fx-background-color: #FFEBEE; -fx-background-radius: 5px; -fx-padding: 8px;";
        } else if (task.getPriority() == Task.Priority.HIGH) {
            return "-fx-background-color: #FFF3E0; -fx-background-radius: 5px; -fx-padding: 8px;";
        }
        return "-fx-background-color: #F8F9FA; -fx-background-radius: 5px; -fx-padding: 8px;";
    }

    private void setupComboBoxes() {
        priorityCombo.getItems().addAll(Task.Priority.values());
        statusCombo.getItems().addAll(Task.Status.values());

        filterPriorityCombo.getItems().add(null); // For "All" option
        filterPriorityCombo.getItems().addAll(Task.Priority.values());

        filterStatusCombo.getItems().add(null); // For "All" option
        filterStatusCombo.getItems().addAll(Task.Status.values());

        // FIXED: Removed Created Date options
        sortCombo.getItems().addAll(
                "Title (A-Z)", "Title (Z-A)",
                "Priority (High-Low)", "Priority (Low-High)",
                "Due Date (Earliest)", "Due Date (Latest)"
        );
        sortCombo.setValue("Priority (High-Low)");
    }

    private void setupButtons() {
        newTaskButton.setOnAction(e -> createNewTask());
        saveTaskButton.setOnAction(e -> saveCurrentTask());
        deleteTaskButton.setOnAction(e -> deleteCurrentTask());

        // Initially disable delete button
        deleteTaskButton.setDisable(true);
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        filterPriorityCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterStatusCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        showCompletedCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadTasks() {
        int userId = UserSession.getInstance().getCurrentUser().getId();
        List<Task> tasks = taskService.getTasksForUser(userId);
        allTasks.setAll(tasks);
        applyFilters();
    }

    private void applyFilters() {
        List<Task> filteredTasks = allTasks.stream()
                .filter(task -> {
                    // Search filter
                    String searchText = searchField.getText();
                    if (searchText != null && !searchText.trim().isEmpty()) {
                        String search = searchText.toLowerCase();
                        if (!task.getTitle().toLowerCase().contains(search) &&
                                !task.getDescription().toLowerCase().contains(search)) {
                            return false;
                        }
                    }

                    // Priority filter
                    Task.Priority priorityFilter = filterPriorityCombo.getValue();
                    if (priorityFilter != null && task.getPriority() != priorityFilter) {
                        return false;
                    }

                    // Status filter
                    Task.Status statusFilter = filterStatusCombo.getValue();
                    if (statusFilter != null && task.getStatus() != statusFilter) {
                        return false;
                    }

                    // Show completed filter
                    if (!showCompletedCheckBox.isSelected() && task.getStatus() == Task.Status.COMPLETED) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Apply sorting
        String sortOption = sortCombo.getValue();
        if (sortOption != null) {
            switch (sortOption) {
                case "Title (A-Z)" -> filteredTasks.sort((t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()));
                case "Title (Z-A)" -> filteredTasks.sort((t1, t2) -> t2.getTitle().compareToIgnoreCase(t1.getTitle()));
                case "Priority (High-Low)" -> filteredTasks.sort((t1, t2) -> t2.getPriority().compareTo(t1.getPriority()));
                case "Priority (Low-High)" -> filteredTasks.sort((t1, t2) -> t1.getPriority().compareTo(t2.getPriority()));
                case "Due Date (Earliest)" -> filteredTasks.sort((t1, t2) -> {
                    if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                    if (t1.getDueDate() == null) return 1;
                    if (t2.getDueDate() == null) return -1;
                    return t1.getDueDate().compareTo(t2.getDueDate());
                });
                case "Due Date (Latest)" -> filteredTasks.sort((t1, t2) -> {
                    if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                    if (t1.getDueDate() == null) return -1;
                    if (t2.getDueDate() == null) return 1;
                    return t2.getDueDate().compareTo(t1.getDueDate());
                });
            }
        }

        tasksList.getItems().setAll(filteredTasks);
        updateStatistics();
    }

    private void loadTaskDetails(Task task) {
        currentTask = task;
        taskTitleField.setText(task.getTitle());
        taskDescriptionArea.setText(task.getDescription());
        priorityCombo.setValue(task.getPriority());
        statusCombo.setValue(task.getStatus());
        dueDatePicker.setValue(task.getDueDate());
    }

    private void createNewTask() {
        currentTask = null;
        taskTitleField.clear();
        taskDescriptionArea.clear();
        priorityCombo.setValue(Task.Priority.MEDIUM);
        statusCombo.setValue(Task.Status.PENDING);
        dueDatePicker.setValue(null);

        tasksList.getSelectionModel().clearSelection();
    }

    private void saveCurrentTask() {
        String title = taskTitleField.getText().trim();
        String description = taskDescriptionArea.getText().trim();
        Task.Priority priority = priorityCombo.getValue();
        Task.Status status = statusCombo.getValue();
        LocalDate dueDate = dueDatePicker.getValue();

        if (title.isEmpty()) {
            NotificationManager.getInstance().showNotification(
                    "Validation Error",
                    "Task title cannot be empty",
                    NotificationManager.NotificationType.WARNING
            );
            return;
        }

        Task task;
        if (currentTask == null) {
            task = new Task(title, description, priority, dueDate);
            task.setUserId(UserSession.getInstance().getCurrentUser().getId());
        } else {
            task = currentTask;
            task.setTitle(title);
            task.setDescription(description);
            task.setPriority(priority);
            task.setDueDate(dueDate);
        }

        task.setStatus(status);

        boolean success;
        if (currentTask == null) {
            success = taskService.addTask(task);
        } else {
            success = taskService.updateTask(task);
        }

        if (success) {
            NotificationManager.getInstance().showNotification(
                    "Task Saved",
                    "Your task has been saved successfully!",
                    NotificationManager.NotificationType.SUCCESS
            );
            loadTasks();
        } else {
            NotificationManager.getInstance().showNotification(
                    "Error",
                    "Failed to save task",
                    NotificationManager.NotificationType.ERROR
            );
        }
    }

    private void deleteCurrentTask() {
        Task selectedTask = tasksList.getSelectionModel().getSelectedItem();

        if (selectedTask == null) {
            NotificationManager.getInstance().showNotification(
                    "No Selection",
                    "Please select a task to delete",
                    NotificationManager.NotificationType.WARNING
            );
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Task");
        alert.setHeaderText("Are you sure you want to delete this task?");
        alert.setContentText("Task: \"" + selectedTask.getTitle() + "\"\nThis action cannot be undone.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (taskService.deleteTask(selectedTask.getId())) {
                    NotificationManager.getInstance().showNotification(
                            "Task Deleted",
                            "Task \"" + selectedTask.getTitle() + "\" has been deleted successfully",
                            NotificationManager.NotificationType.SUCCESS
                    );
                    loadTasks();
                    createNewTask();
                }
            }
        });
    }

    private void updateStatistics() {
        List<Task> currentTasks = tasksList.getItems();

        int total = allTasks.size();
        int pending = (int) allTasks.stream().filter(t -> t.getStatus() == Task.Status.PENDING).count();
        int completed = (int) allTasks.stream().filter(t -> t.getStatus() == Task.Status.COMPLETED).count();
        int overdue = (int) allTasks.stream()
                .filter(t -> t.getDueDate() != null &&
                        t.getDueDate().isBefore(LocalDate.now()) &&
                        t.getStatus() != Task.Status.COMPLETED)
                .count();

        totalTasksLabel.setText("Total: " + total);
        pendingTasksLabel.setText("Pending: " + pending);
        completedTasksLabel.setText("Completed: " + completed);
        overdueTasksLabel.setText("Overdue: " + overdue);
    }
}
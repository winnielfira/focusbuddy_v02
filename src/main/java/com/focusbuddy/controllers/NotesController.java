package com.focusbuddy.controllers;

import com.focusbuddy.models.Note;
import com.focusbuddy.services.NotesService;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;

import java.util.List;

public class NotesController {

    @FXML private VBox notesContainer;
    @FXML private ListView<Note> notesList;
    @FXML private TextField noteTitleField;
    @FXML private HTMLEditor noteEditor;
    @FXML private Button saveNoteButton;
    @FXML private Button newNoteButton;
    @FXML private Button deleteNoteButton;
    @FXML private TextField searchField;
    @FXML private TextField tagsField;

    // Removed formatting buttons and color picker
    // @FXML private Button boldButton;
    // @FXML private Button italicButton;
    // @FXML private Button highlightButton;
    // @FXML private ColorPicker highlightColorPicker;

    private NotesService notesService;
    private Note currentNote;

    @FXML
    private void initialize() {
        notesService = new NotesService();

        setupNotesList();
        setupButtons();
        setupSearch();
        loadNotes();
    }

    private void setupNotesList() {
        notesList.setCellFactory(listView -> new ListCell<Note>() {
            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(""); // Reset style
                } else {
                    setText(note.getTitle());

                    // Add selection highlighting similar to tasks
                    if (isSelected()) {
                        setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-width: 2px; -fx-padding: 8px;");
                    } else {
                        setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8px;");
                    }
                }
            }
        });

        notesList.getSelectionModel().selectedItemProperty().addListener((obs, oldNote, newNote) -> {
            if (newNote != null) {
                loadNoteContent(newNote);
                // Enable delete button when something is selected
                deleteNoteButton.setDisable(false);
                deleteNoteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            } else {
                // Disable delete button when nothing is selected
                deleteNoteButton.setDisable(true);
                deleteNoteButton.setStyle("");
            }
        });
    }

    private void setupButtons() {
        newNoteButton.setOnAction(e -> createNewNote());
        saveNoteButton.setOnAction(e -> saveCurrentNote());
        deleteNoteButton.setOnAction(e -> deleteCurrentNote());

        // Initially disable delete button
        deleteNoteButton.setDisable(true);

        // Removed formatting button setups
        // boldButton.setOnAction(e -> applyBoldFormatting());
        // italicButton.setOnAction(e -> applyItalicFormatting());
        // highlightButton.setOnAction(e -> applyHighlightFormatting());
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            searchNotes(newText);
        });
    }

    private void loadNotes() {
        int userId = UserSession.getInstance().getCurrentUser().getId();
        List<Note> notes = notesService.getNotesForUser(userId);
        notesList.getItems().setAll(notes);
    }

    private void loadNoteContent(Note note) {
        currentNote = note;
        noteTitleField.setText(note.getTitle());
        noteEditor.setHtmlText(note.getContent());
        tagsField.setText(note.getTags() != null ? note.getTags() : "");
    }

    private void createNewNote() {
        currentNote = new Note();
        currentNote.setUserId(UserSession.getInstance().getCurrentUser().getId());
        currentNote.setTitle("New Note");
        currentNote.setContent("");

        noteTitleField.setText("New Note");
        noteEditor.setHtmlText("");
        tagsField.setText("");

        notesList.getSelectionModel().clearSelection();
    }

    private void saveCurrentNote() {
        if (currentNote == null) {
            createNewNote();
        }

        String title = noteTitleField.getText().trim();
        String content = noteEditor.getHtmlText();
        String tags = tagsField.getText().trim();

        if (title.isEmpty()) {
            NotificationManager.getInstance().showNotification(
                    "Validation Error",
                    "Note title cannot be empty",
                    NotificationManager.NotificationType.WARNING
            );
            return;
        }

        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.setTags(tags);

        boolean success;
        if (currentNote.getId() == 0) {
            success = notesService.createNote(currentNote);
        } else {
            success = notesService.updateNote(currentNote);
        }

        if (success) {
            NotificationManager.getInstance().showNotification(
                    "Note Saved",
                    "Your note has been saved successfully!",
                    NotificationManager.NotificationType.SUCCESS
            );
            loadNotes();
        } else {
            NotificationManager.getInstance().showNotification(
                    "Error",
                    "Failed to save note",
                    NotificationManager.NotificationType.ERROR
            );
        }
    }

    private void deleteCurrentNote() {
        Note selectedNote = notesList.getSelectionModel().getSelectedItem();

        if (selectedNote == null) {
            NotificationManager.getInstance().showNotification(
                    "No Selection",
                    "Please select a note to delete",
                    NotificationManager.NotificationType.WARNING
            );
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Note");
        alert.setHeaderText("Are you sure you want to delete this note?");
        alert.setContentText("Note: \"" + selectedNote.getTitle() + "\"\nThis action cannot be undone.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (notesService.deleteNote(selectedNote.getId())) {
                    NotificationManager.getInstance().showNotification(
                            "Note Deleted",
                            "Note \"" + selectedNote.getTitle() + "\" has been deleted successfully",
                            NotificationManager.NotificationType.SUCCESS
                    );
                    loadNotes();
                    createNewNote();
                }
            }
        });
    }

    private void searchNotes(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            loadNotes();
            return;
        }

        int userId = UserSession.getInstance().getCurrentUser().getId();
        List<Note> searchResults = notesService.searchNotes(userId, searchText);
        notesList.getItems().setAll(searchResults);
    }

    // Removed formatting methods:
    // private void applyBoldFormatting() { ... }
    // private void applyItalicFormatting() { ... }
    // private void applyHighlightFormatting() { ... }
    // private String getSelectedText() { ... }
    // private void replaceSelectedText(String newText) { ... }
}
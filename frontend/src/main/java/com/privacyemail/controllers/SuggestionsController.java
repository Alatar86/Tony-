package com.privacyemail.controllers;

import com.privacyemail.models.EmailDetails;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the suggestions window.
 */
public class SuggestionsController {

    private static final Logger logger = LoggerFactory.getLogger(SuggestionsController.class);

    @FXML private VBox suggestion1Pane;
    @FXML private VBox suggestion2Pane;
    @FXML private VBox suggestion3Pane;

    @FXML private Label suggestion1Label;
    @FXML private Label suggestion2Label;
    @FXML private Label suggestion3Label;

    @FXML private Button cancelButton;

    private Consumer<String> selectionCallback;
    private List<String> suggestions;

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing SuggestionsController");

        // Set up hover effects and click handlers for each suggestion pane
        setupSuggestionPane(suggestion1Pane, suggestion1Label, 0);
        setupSuggestionPane(suggestion2Pane, suggestion2Label, 1);
        setupSuggestionPane(suggestion3Pane, suggestion3Label, 2);
    }

    /**
     * Set up a suggestion pane with hover effects and click handler.
     *
     * @param pane The suggestion pane
     * @param label The suggestion text label
     * @param index The index of the suggestion in the suggestions list
     */
    private void setupSuggestionPane(VBox pane, Label label, int index) {
        // Hover effect
        pane.setOnMouseEntered(e -> {
            pane.setStyle(pane.getStyle() + "-fx-background-color: #d4e9f7; -fx-border-width: 2;");
            label.setStyle(label.getStyle() + "-fx-font-weight: bold;");
        });

        pane.setOnMouseExited(e -> {
            pane.setStyle(pane.getStyle().replace("-fx-background-color: #d4e9f7; -fx-border-width: 2;", ""));
            label.setStyle(label.getStyle().replace("-fx-font-weight: bold;", ""));
        });

        // Click handler
        pane.setOnMouseClicked(e -> {
            handleSuggestionClick(index);
        });
    }

    /**
     * Set the suggestions for display and configure the callback.
     *
     * @param suggestions The list of suggestion strings
     * @param originalEmail The original email being replied to
     * @param selectionCallback Callback for when a suggestion is selected
     */
    public void setSuggestions(List<String> suggestions, EmailDetails originalEmail, Consumer<String> selectionCallback) {
        this.selectionCallback = selectionCallback;

        // Clean up suggestions for display
        List<String> cleanedSuggestions = new ArrayList<>();
        for (String suggestion : suggestions) {
            // Clean each suggestion
            String cleaned = cleanSuggestionText(suggestion);
            if (!cleaned.isEmpty()) {
                cleanedSuggestions.add(cleaned);
            }
        }

        // Ensure we have exactly 3 suggestions (pad with defaults if needed)
        while (cleanedSuggestions.size() < 3) {
            cleanedSuggestions.add("Thank you for your email. I'll respond shortly.");
        }

        // Keep only the first 3 if we have more
        if (cleanedSuggestions.size() > 3) {
            cleanedSuggestions = cleanedSuggestions.subList(0, 3);
        }

        // Store the cleaned suggestions
        this.suggestions = cleanedSuggestions;

        // Populate the suggestion labels
        suggestion1Label.setText(this.suggestions.get(0));
        suggestion2Label.setText(this.suggestions.get(1));
        suggestion3Label.setText(this.suggestions.get(2));

        logger.info("Suggestions loaded: {}", this.suggestions);
    }

    /**
     * Clean suggestion text for display.
     *
     * @param text The suggestion text to clean
     * @return Cleaned suggestion text
     */
    private String cleanSuggestionText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Remove numbering (e.g., "1. ", "1: ", etc.)
        text = text.replaceAll("^\\s*\\d+[.:]\\s*", "");

        // Remove quotes if they wrap the entire suggestion
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }

        // Remove any remaining instructional text
        text = text.replaceAll("(?i)^\\s*I suggest\\s+", "");
        text = text.replaceAll("(?i)^\\s*You could say\\s+", "");
        text = text.replaceAll("(?i)^\\s*Consider saying\\s+", "");

        return text.trim();
    }

    /**
     * Handle the Cancel button action.
     */
    @FXML
    private void handleCancelAction() {
        closeWindow();
    }

    /**
     * Close the window.
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Handle click on a suggestion pane.
     *
     * @param index The index of the suggestion clicked
     */
    private void handleSuggestionClick(int index) {
        if (suggestions != null && index < suggestions.size() && selectionCallback != null) {
            // Get the selected suggestion text
            String selectedSuggestion = suggestions.get(index);

            // Pass it to the callback
            selectionCallback.accept(selectedSuggestion);

            // Close the window
            closeWindow();
        }
    }
}

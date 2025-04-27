package com.privacyemail.controllers;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import com.privacyemail.models.EmailMetadata;

/**
 * Custom ListCell for rendering EmailMetadata in the email list.
 */
public class EmailListCell extends ListCell<EmailMetadata> {

    // Declare fields, but don't initialize here
    private HBox container;
    private VBox contentBox;
    private Label subjectLabel;
    private Label senderLabel;
    private Label dateLabel;

    /**
     * Create a new EmailListCell.
     */
    public EmailListCell() {
        // Initialize all fields to null explicitly to address warnings
        container = null;
        contentBox = null;
        subjectLabel = null;
        senderLabel = null;
        dateLabel = null;
    }

    @Override
    protected void updateItem(EmailMetadata email, boolean empty) {
        super.updateItem(email, empty);

        // Clear previous content
        setText(null);
        setGraphic(null);

        if (empty || email == null) {
            // Keep cell empty
            return;
        }

        // Lazy initialization
        if (container == null) {
            // Create and configure nodes once
            container = new HBox();
            container.setSpacing(5); // Reduced spacing
            container.setPadding(new Insets(5)); // Reduced padding
            container.setAlignment(Pos.CENTER_LEFT);

            contentBox = new VBox();
            contentBox.setSpacing(1); // Minimal spacing
            HBox.setHgrow(contentBox, Priority.ALWAYS);

            subjectLabel = new Label();
            subjectLabel.getStyleClass().add("email-subject");
            subjectLabel.setStyle("-fx-font-weight: bold;");

            senderLabel = new Label();
            senderLabel.getStyleClass().add("email-sender");
            senderLabel.setStyle("-fx-font-size: 90%;");

            dateLabel = new Label();
            dateLabel.getStyleClass().add("email-date");
            dateLabel.setStyle("-fx-font-size: 85%;");
            dateLabel.setAlignment(Pos.CENTER_RIGHT);
            dateLabel.setMinWidth(60); // Give date label a minimum width

            contentBox.getChildren().addAll(subjectLabel, senderLabel);
            container.getChildren().addAll(contentBox, dateLabel);
        }

        // Update content of existing nodes
        subjectLabel.setText(email.getSubject());
        senderLabel.setText(email.getFromAddress());

        // Format date
        dateLabel.setText(formatDate(email.getDate()));

        // Set the graphic
        setGraphic(container);
    }

    /**
     * Format the email date for display.
     *
     * @param dateString The date string from the email
     * @return Formatted date string
     */
    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }

        // Check if date string contains time
        if (dateString.contains(":")) {
            // Extract time
            String[] parts = dateString.split(" ");
            for (String part : parts) {
                if (part.contains(":")) {
                    return part.substring(0, Math.min(5, part.length())); // Just HH:MM
                }
            }
        }

        // If we can't extract time, return a shortened date string
        if (dateString.length() > 10) {
            return dateString.substring(0, 10); // Just return first 10 chars
        }

        return dateString;
    }
}

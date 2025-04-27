package com.privacyemail.ui;

import com.google.inject.Inject;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages the folder navigation sidebar, handling clicks and UI updates.
 */
public class FolderNavigationManager {

    private static final Logger logger = LoggerFactory.getLogger(FolderNavigationManager.class);

    private final Label folderTitleLabel;
    private final Map<String, HBox> folderBoxes; // Map Label ID to HBox
    private final Consumer<String> onFolderSelectedCallback;

    private String currentLabelId = "INBOX"; // Default to INBOX

    @Inject
    public FolderNavigationManager(Label folderTitleLabel,
                                   Map<String, HBox> folderBoxes,
                                   Consumer<String> onFolderSelectedCallback) {
        this.folderTitleLabel = folderTitleLabel;
        this.folderBoxes = folderBoxes.entrySet().stream()
                                      .filter(entry -> entry.getValue() != null) // Filter out null HBoxes
                                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.onFolderSelectedCallback = onFolderSelectedCallback;

        setupFolderClickHandlers();
        updateFolderTitle(); // Initial title update
        updateActiveFolderStyle(); // Initial style update
    }

    /**
     * Sets up the mouse click handlers for each folder HBox.
     */
    private void setupFolderClickHandlers() {
        folderBoxes.forEach((labelId, hbox) -> {
            if (hbox != null) {
                hbox.setOnMouseClicked(e -> handleFolderClick(labelId));
            }
        });
    }

    /**
     * Handles the logic when a folder HBox is clicked.
     * @param selectedLabelId The Gmail Label ID of the clicked folder.
     */
    private void handleFolderClick(String selectedLabelId) {
        if (!selectedLabelId.equals(currentLabelId)) {
            logger.info("FolderNavigationManager: Selecting folder with Label ID: {}", selectedLabelId);
            currentLabelId = selectedLabelId;
            updateFolderTitle();
            updateActiveFolderStyle();

            // Execute the callback provided by MainWindowController
            if (onFolderSelectedCallback != null) {
                onFolderSelectedCallback.accept(selectedLabelId);
            }
        }
    }

    /**
     * Updates the folder title label based on the currentLabelId.
     */
    private void updateFolderTitle() {
        if (folderTitleLabel != null) {
            String displayName = getFolderDisplayName(currentLabelId);
            folderTitleLabel.setText(displayName);
        }
    }

    /**
     * Updates the visual styling (CSS class) of the folder HBoxes
     * to highlight the currently selected folder.
     */
    private void updateActiveFolderStyle() {
        // Remove selected class from all folders
        folderBoxes.values().forEach(hbox -> {
            if (hbox != null) {
                hbox.getStyleClass().remove("selected");
            }
        });

        // Add selected class to the current folder's HBox
        HBox selectedBox = folderBoxes.get(currentLabelId);
        if (selectedBox != null) {
            selectedBox.getStyleClass().add("selected");
        } else {
             logger.warn("No HBox found for currentLabelId: {}", currentLabelId);
        }
    }

    /**
     * Converts standard Gmail Label IDs to user-friendly display names.
     * @param labelId The Gmail Label ID.
     * @return A user-friendly display name.
     */
    private String getFolderDisplayName(String labelId) {
        // TODO: Consider making this mapping more extensible if custom labels are added
        switch (labelId) {
            case "INBOX": return "Inbox";
            case "STARRED": return "Starred";
            case "SENT": return "Sent";
            case "DRAFT": return "Drafts";
            case "SPAM": return "Spam";
            case "TRASH": return "Trash";
            // Add mappings for any other standard/custom labels used
            // e.g., "CATEGORY_UPDATES", "IMPORTANT", etc.
            default: return labelId; // Fallback to label ID itself
        }
    }

    /**
     * Gets the currently selected label ID.
     * @return The current label ID.
     */
    public String getCurrentLabelId() {
        return currentLabelId;
    }

    // Optional: Method to programmatically select a folder if needed
    public void selectFolder(String labelId) {
         handleFolderClick(labelId);
    }
}

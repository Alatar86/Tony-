package com.privacyemail.controllers;

import javafx.scene.control.ListCell;

/**
 * Custom ListCell for rendering suggestion items in the suggestions list.
 */
public class SuggestionListCell extends ListCell<String> {

    /**
     * Create a new SuggestionListCell.
     */
    public SuggestionListCell() {
        // Explicit constructor to address initialization warning
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            // Placeholder: Set text/graphic for the suggestion string
            setText(item);
        }
    }
}

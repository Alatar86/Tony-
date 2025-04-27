package com.privacyemail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
// import com.fasterxml.jackson.annotation.JsonProperty; // Unused?
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the response from the suggestion generation API.
 * Contains a list of suggested replies.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuggestionResponse {

    private List<String> suggestions;

    /**
     * Default constructor that initializes with an empty suggestion list.
     */
    public SuggestionResponse() {
        this.suggestions = new ArrayList<>();
    }

    /**
     * Creates a new SuggestionResponse with the given suggestions.
     *
     * @param suggestions The list of suggested replies
     */
    public SuggestionResponse(List<String> suggestions) {
        this.suggestions = new ArrayList<>();
        if (suggestions != null) {
            this.suggestions.addAll(suggestions);
        }
    }

    /**
     * Gets the list of suggested replies.
     *
     * @return An unmodifiable list of suggestions
     */
    public List<String> getSuggestions() {
        return Collections.unmodifiableList(suggestions);
    }

    /**
     * Sets the list of suggested replies.
     *
     * @param suggestions The list of suggested replies
     */
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = new ArrayList<>();
        if (suggestions != null) {
            this.suggestions.addAll(suggestions);
        }
    }

    /**
     * Adds a suggestion to the list.
     *
     * @param suggestion The suggestion to add
     */
    public void addSuggestion(String suggestion) {
        if (suggestion != null && !suggestion.isEmpty()) {
            this.suggestions.add(suggestion);
        }
    }

    /**
     * Checks if there are any suggestions.
     *
     * @return true if there are suggestions, false otherwise
     */
    public boolean hasSuggestions() {
        return !suggestions.isEmpty();
    }

    /**
     * Gets the number of suggestions.
     *
     * @return The number of suggestions
     */
    public int getCount() {
        return suggestions.size();
    }
}

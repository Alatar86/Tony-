package com.privacyemail.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the response from the backend status endpoint.
 */
public class StatusResponse {

    @JsonProperty("gmail_authenticated")
    private boolean gmail_authenticated;

    @JsonProperty("local_ai_service_status")
    private String local_ai_service_status;

    /**
     * Default constructor for JSON deserialization.
     */
    public StatusResponse() {
    }

    /**
     * Create a new StatusResponse instance.
     *
     * @param gmail_authenticated Gmail authentication status
     * @param local_ai_service_status AI service status (active/inactive/error)
     */
    public StatusResponse(boolean gmail_authenticated, String local_ai_service_status) {
        this.gmail_authenticated = gmail_authenticated;
        this.local_ai_service_status = local_ai_service_status;
    }

    /**
     * Get the Gmail authentication status.
     *
     * @return true if Gmail is authenticated, false otherwise
     */
    public boolean isGmail_authenticated() {
        return gmail_authenticated;
    }

    /**
     * Set the Gmail authentication status.
     *
     * @param gmail_authenticated Authentication status
     */
    public void setGmail_authenticated(boolean gmail_authenticated) {
        this.gmail_authenticated = gmail_authenticated;
    }

    /**
     * Get the local AI service status.
     *
     * @return Status string (active/inactive/error)
     */
    public String getLocal_ai_service_status() {
        return local_ai_service_status;
    }

    /**
     * Set the local AI service status.
     *
     * @param local_ai_service_status Status string
     */
    public void setLocal_ai_service_status(String local_ai_service_status) {
        this.local_ai_service_status = local_ai_service_status;
    }
}

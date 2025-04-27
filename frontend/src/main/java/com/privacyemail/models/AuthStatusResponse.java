package com.privacyemail.models;

/**
 * Represents the response from the authentication status endpoint.
 */
public class AuthStatusResponse {

    private boolean authenticated;

    /**
     * Default constructor for JSON deserialization.
     */
    public AuthStatusResponse() {
    }

    /**
     * Create a new AuthStatusResponse instance.
     *
     * @param authenticated Authentication status
     */
    public AuthStatusResponse(boolean authenticated) {
        this.authenticated = authenticated;
    }

    /**
     * Get the authentication status.
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Set the authentication status.
     *
     * @param authenticated Authentication status
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}

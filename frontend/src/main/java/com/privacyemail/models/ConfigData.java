package com.privacyemail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration data for the application loaded from the backend.
 * Contains various configuration sections for different parts of the application.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigData(
    @JsonProperty("Ollama") OllamaConfig ollama,
    @JsonProperty("App") AppConfig app,
    @JsonProperty("User") UserConfig user
) {
    /**
     * Configuration for the Ollama large language model service.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaConfig(
        @JsonProperty("api_base_url") String api_base_url,
        @JsonProperty("model_name") String model_name
    ) {}

    /**
     * General application configuration.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AppConfig(
        @JsonProperty("max_emails_fetch") int max_emails_fetch
    ) {}

    /**
     * User-specific configuration.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserConfig(
        @JsonProperty("signature") String signature
    ) {}
}

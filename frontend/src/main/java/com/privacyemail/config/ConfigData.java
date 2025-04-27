package com.privacyemail.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data class representing the application configuration data
 * loaded from the backend server.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigData(
    @JsonProperty("api_key") String apiKey,
    @JsonProperty("backend_url") String backendUrl,
    @JsonProperty("server_info") ServerInfo serverInfo,
    @JsonProperty("smtp_config") SmtpConfig smtpConfig,
    @JsonProperty("imap_config") ImapConfig imapConfig,
    @JsonProperty("user_config") UserConfig userConfig
) {
    /**
     * Server information details
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ServerInfo(
        @JsonProperty("version") String version,
        @JsonProperty("environment") String environment
    ) {}

    /**
     * SMTP server configuration
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SmtpConfig(
        @JsonProperty("host") String host,
        @JsonProperty("port") int port,
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("use_ssl") boolean useSsl,
        @JsonProperty("use_tls") boolean useTls
    ) {}

    /**
     * IMAP server configuration
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImapConfig(
        @JsonProperty("host") String host,
        @JsonProperty("port") int port,
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("folders") List<String> folders,
        @JsonProperty("use_ssl") boolean useSsl
    ) {}

    /**
     * User-specific configuration
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserConfig(
        @JsonProperty("signature") String signature
    ) {}
}

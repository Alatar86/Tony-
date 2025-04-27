package com.privacyemail.plugins;

import com.privacyemail.api.EmailPlugin;
import com.privacyemail.config.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Factory class for creating LinkSanitizerPlugin instances.
 * Provides methods to load trusted domains from configuration
 * and create plugin instances with customized settings.
 */
public class LinkSanitizerPluginFactory {
    private static final Logger logger = LoggerFactory.getLogger(LinkSanitizerPluginFactory.class);
    private static final String TRUSTED_DOMAINS_FILE = "/com/privacyemail/config/trusted_domains.txt";

    /**
     * Create a LinkSanitizerPlugin with default settings
     * @return a new LinkSanitizerPlugin instance
     */
    public static EmailPlugin createDefaultPlugin() {
        Set<String> trustedDomains = loadTrustedDomains();
        LinkSanitizerPlugin plugin = new LinkSanitizerPlugin();
        plugin.setTrustedDomains(trustedDomains);
        return plugin;
    }

    /**
     * Create a LinkSanitizerPlugin with the specified trusted domains
     * @param trustedDomains set of trusted domain names
     * @return a new LinkSanitizerPlugin instance
     */
    public static EmailPlugin createPluginWithDomains(Set<String> trustedDomains) {
        LinkSanitizerPlugin plugin = new LinkSanitizerPlugin();
        plugin.setTrustedDomains(trustedDomains);
        return plugin;
    }

    /**
     * Load trusted domains from configuration file
     * @return set of trusted domain names
     */
    public static Set<String> loadTrustedDomains() {
        Set<String> domains = new HashSet<>();

        // Add default trusted domains
        domains.add("gmail.com");
        domains.add("google.com");
        domains.add("microsoft.com");
        domains.add("outlook.com");
        domains.add("yahoo.com");

        // Try to load domains from configuration file
        try (InputStream is = LinkSanitizerPluginFactory.class.getResourceAsStream(TRUSTED_DOMAINS_FILE)) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        domains.add(line);
                    }
                }
                logger.info("Loaded {} trusted domains from configuration", domains.size());
            } else {
                logger.warn("Trusted domains configuration file not found: {}", TRUSTED_DOMAINS_FILE);
            }
        } catch (IOException e) {
            logger.error("Error loading trusted domains from configuration", e);
        }

        return domains;
    }

    /**
     * Add a trusted domain to the user's preferences
     * @param domain the domain to add
     */
    public static void addTrustedDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }

        domain = domain.trim().toLowerCase();

        // For actual implementation, you would want to have a preference key for trusted domains
        // and store them in a structured format (like a comma-separated list or JSON).
        // This is a placeholder for demonstration purposes.
        UserPreferences prefs = UserPreferences.getInstance();
        // In a real implementation, this would use a proper API for preference lists
        logger.info("Added trusted domain: {}", domain);
    }

    /**
     * Remove a trusted domain from the user's preferences
     * @param domain the domain to remove
     */
    public static void removeTrustedDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }

        domain = domain.trim().toLowerCase();

        // For actual implementation, you would want to have a preference key for trusted domains
        // and remove the domain from the structured format.
        // This is a placeholder for demonstration purposes.
        UserPreferences prefs = UserPreferences.getInstance();
        // In a real implementation, this would use a proper API for preference lists
        logger.info("Removed trusted domain: {}", domain);
    }
}

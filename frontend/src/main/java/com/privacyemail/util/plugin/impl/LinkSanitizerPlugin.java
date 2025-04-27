package com.privacyemail.util.plugin.impl;

import com.privacyemail.models.EmailDetails;
import com.privacyemail.util.plugin.EmailViewPlugin;
import javafx.scene.web.WebView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin that sanitizes links in emails by converting them to redirect through a safety checker.
 * This helps protect users from phishing and malicious links.
 */
public class LinkSanitizerPlugin implements EmailViewPlugin {
    private static final Logger LOGGER = Logger.getLogger(LinkSanitizerPlugin.class.getName());
    private static final String PLUGIN_ID = "com.privacyemail.plugin.linksanitizer";
    private static final String PLUGIN_NAME = "Link Sanitizer";
    private static final String PLUGIN_DESCRIPTION = "Sanitizes links in emails to protect against phishing";
    private static final String PLUGIN_VERSION = "1.0";

    private static final String REDIRECT_BASE_URL = "https://saferedirect.privacyemail.com/check?url=";

    private WebView webView;
    private boolean enabled = true;

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getDisplayName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return PLUGIN_DESCRIPTION;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public void initialize(WebView webView) {
        this.webView = webView;
        LOGGER.info("Initialized LinkSanitizerPlugin");
    }

    @Override
    public String processContent(String content, EmailDetails emailDetails) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        try {
            Document doc = Jsoup.parse(content);

            // Process anchor tags
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String href = link.attr("href");

                // Skip javascript, mailto, tel links
                if (href.startsWith("javascript:") ||
                    href.startsWith("mailto:") ||
                    href.startsWith("tel:") ||
                    href.startsWith("#")) {
                    continue;
                }

                // Add data attribute with original URL for reference
                link.attr("data-original-url", href);

                // Replace href with redirect URL
                link.attr("href", REDIRECT_BASE_URL + encodeUrl(href));

                // Add class for styling
                link.addClass("sanitized-link");

                // Add title attribute if not present
                if (!link.hasAttr("title")) {
                    link.attr("title", "This link will be checked for safety when clicked");
                }
            }

            LOGGER.fine("Processed HTML content for link sanitization");
            return doc.html();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing HTML content for link sanitization", e);
            return content; // Return original content on error
        }
    }

    @Override
    public void onEmailLoaded(EmailDetails emailDetails) {
        if (webView != null) {
            try {
                // Add JavaScript to show a safety warning when hovering over sanitized links
                String js = "document.querySelectorAll('.sanitized-link').forEach(link => {" +
                        "  link.addEventListener('mouseover', function() {" +
                        "    this.style.textDecoration = 'underline';" +
                        "    this.style.color = '#007bff';" +
                        "  });" +
                        "  link.addEventListener('mouseout', function() {" +
                        "    this.style.textDecoration = '';" +
                        "    this.style.color = '';" +
                        "  });" +
                        "});";

                webView.getEngine().executeScript(js);
                LOGGER.fine("Added safety warning JavaScript to WebView");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error adding safety warning JavaScript", e);
            }
        }
    }

    @Override
    public void cleanup() {
        webView = null;
        LOGGER.info("Cleaned up LinkSanitizerPlugin");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("LinkSanitizerPlugin " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Simple URL encoding method for use in redirect URLs.
     *
     * @param url The URL to encode
     * @return The encoded URL
     */
    private String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error encoding URL: " + url, e);
            return url;
        }
    }
}

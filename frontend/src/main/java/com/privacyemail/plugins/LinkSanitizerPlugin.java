package com.privacyemail.plugins;

import com.privacyemail.api.EmailPlugin;
import com.privacyemail.api.PluginRegistry;
import javafx.scene.web.WebView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Plugin that sanitizes links in emails to protect users from phishing and malicious links.
 * This plugin:
 * 1. Inspects all links in the email content
 * 2. Adds safety warnings for external domains
 * 3. Highlights potentially suspicious links
 */
public class LinkSanitizerPlugin implements EmailPlugin {
    private static final Logger logger = LoggerFactory.getLogger(LinkSanitizerPlugin.class);
    private static final String PLUGIN_ID = "com.privacyemail.plugins.linksanitizer";
    private static final String PLUGIN_NAME = "Link Sanitizer";
    private static final String PLUGIN_DESCRIPTION = "Sanitizes links in emails to protect from phishing attempts";

    private Set<String> trustedDomains;

    /**
     * Default constructor that initializes with an empty set of trusted domains.
     */
    public LinkSanitizerPlugin() {
        trustedDomains = new HashSet<>();
    }

    /**
     * Set the trusted domains for this plugin.
     * @param domains set of domain names to trust
     */
    public void setTrustedDomains(Set<String> domains) {
        if (domains == null) {
            this.trustedDomains = new HashSet<>();
        } else {
            this.trustedDomains = new HashSet<>(domains);
        }
        logger.info("Set {} trusted domains for link sanitization", this.trustedDomains.size());
    }

    /**
     * Get the current set of trusted domains.
     * @return unmodifiable set of trusted domains
     */
    public Set<String> getTrustedDomains() {
        return Collections.unmodifiableSet(trustedDomains);
    }

    /**
     * Add a domain to the trusted domains list.
     * @param domain the domain name to add
     */
    public void addTrustedDomain(String domain) {
        if (domain != null && !domain.trim().isEmpty()) {
            trustedDomains.add(domain.trim().toLowerCase());
            logger.debug("Added trusted domain: {}", domain);
        }
    }

    /**
     * Remove a domain from the trusted domains list.
     * @param domain the domain name to remove
     * @return true if the domain was in the list and removed
     */
    public boolean removeTrustedDomain(String domain) {
        if (domain != null && !domain.trim().isEmpty()) {
            boolean removed = trustedDomains.remove(domain.trim().toLowerCase());
            if (removed) {
                logger.debug("Removed trusted domain: {}", domain);
            }
            return removed;
        }
        return false;
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing Link Sanitizer Plugin");
        PluginRegistry.getInstance().registerPlugin(this);
    }

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return PLUGIN_DESCRIPTION;
    }

    @Override
    public String processEmailContent(String htmlContent, String emailId) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return htmlContent;
        }

        try {
            Document doc = Jsoup.parse(htmlContent);
            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String href = link.attr("href");
                if (href.startsWith("http://") || href.startsWith("https://")) {
                    processExternalLink(link, href);
                }
            }

            // Add our safety JavaScript to the document
            addSafetyJavaScript(doc);

            return doc.outerHtml();
        } catch (Exception e) {
            logger.error("Error processing links in email content for email: {}", emailId, e);
            return htmlContent; // Return original content if processing fails
        }
    }

    private void processExternalLink(Element link, String href) {
        try {
            URI uri = new URI(href);
            String domain = uri.getHost();

            // Visual indicator based on trust level
            if (domain != null) {
                domain = domain.toLowerCase();
                if (trustedDomains.contains(domain)) {
                    link.addClass("trusted-link");
                } else {
                    link.addClass("external-link");
                    // Add safety attributes
                    link.attr("data-original-url", href);
                    link.attr("onclick", "confirmNavigation(this.getAttribute('data-original-url')); return false;");
                }
            }
        } catch (URISyntaxException e) {
            logger.warn("Invalid URL found in email: {}", href);
            link.addClass("invalid-link");
        }
    }

    private void addSafetyJavaScript(Document doc) {
        Element head = doc.head();
        if (head == null) {
            head = doc.createElement("head");
            doc.prependChild(head);
        }

        Element script = doc.createElement("script");
        script.attr("type", "text/javascript");
        script.appendText(
            "function confirmNavigation(url) {\n" +
            "  if (confirm('This link will take you to an external website: ' + url + '\\n\\nDo you want to proceed?')) {\n" +
            "    window.open(url, '_blank');\n" +
            "  }\n" +
            "}\n"
        );
        head.appendChild(script);

        Element style = doc.createElement("style");
        style.appendText(
            ".external-link { color: #FF8C00; text-decoration: underline; }\n" +
            ".trusted-link { color: #008000; }\n" +
            ".invalid-link { color: #FF0000; text-decoration: line-through; }\n"
        );
        head.appendChild(style);
    }

    @Override
    public void configureWebView(WebView webView, String emailId) {
        // Additional WebView configuration could be added here
        logger.debug("Configuring WebView for link sanitization for email: {}", emailId);
    }

    @Override
    public void cleanup() {
        logger.info("Cleaning up Link Sanitizer Plugin");
        trustedDomains.clear();
    }
}

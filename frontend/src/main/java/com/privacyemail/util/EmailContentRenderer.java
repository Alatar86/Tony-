package com.privacyemail.util;

// import javafx.scene.web.WebView; // Unused according to linter
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.privacyemail.config.FrontendPreferences;
// Removed ThemeManager import as methods didn't exist there
// import com.privacyemail.config.ThemeManager;

/**
 * Utility class for rendering email content in a theme-aware manner.
 * Handles HTML sanitization, styling, and plain text conversion.
 */
public class EmailContentRenderer {

    private static final Logger logger = LoggerFactory.getLogger(EmailContentRenderer.class);

    // --- Methods to generate theme-specific styles (Moved/Created Here) ---
    /**
     * Generates CSS styles for HTML email content based on the theme.
     */
    private static String generateEmailContentStyles(boolean isDarkTheme) {
        if (isDarkTheme) {
            return "body.email-body { background-color: #2b2b2b; color: #bbbbbb; font-family: sans-serif; margin: 0; padding: 10px; } " +
                   "a { color: #6897bb; } " +
                   "pre { white-space: pre-wrap; word-wrap: break-word; font-family: monospace; } " +
                   "blockquote { border-left: 3px solid #555; padding-left: 10px; margin-left: 5px; color: #999; }";
        } else {
            return "body.email-body { background-color: #ffffff; color: #333333; font-family: sans-serif; margin: 0; padding: 10px; } " +
                   "a { color: #005a9c; } " +
                   "pre { white-space: pre-wrap; word-wrap: break-word; font-family: monospace; } " +
                   "blockquote { border-left: 3px solid #ccc; padding-left: 10px; margin-left: 5px; color: #555; }";
        }
    }

    /**
     * Generates CSS styles for plain text email content based on the theme.
     */
    // Method is unused according to linter
    // private static String generatePlainTextStyles(boolean isDarkTheme) {
    //     if (isDarkTheme) {
    //         return "body { background-color: #2b2b2b; color: #bbbbbb; font-family: monospace; margin: 0; padding: 10px; } " +
    //                "pre { white-space: pre-wrap; word-wrap: break-word; }";
    //     } else {
    //         return "body { background-color: #ffffff; color: #333333; font-family: monospace; margin: 0; padding: 10px; } " +
    //                "pre { white-space: pre-wrap; word-wrap: break-word; }";
    //     }
    // }
    // --- End of Style Generation Methods ---

    /**
     * Renders HTML email content with proper styling and sanitization.
     *
     * @param htmlContent The original HTML content
     * @param isDarkTheme Whether dark theme is active
     * @param imageLoadingPreference User's preference for loading images
     * @return Processed and styled HTML content
     */
    public static String renderHtmlContent(String htmlContent, boolean isDarkTheme, String imageLoadingPreference) {
        if (htmlContent == null) return "<p>No content</p>";

        // Process content based on image loading preferences
        if (FrontendPreferences.IMAGE_LOADING_NEVER.equals(imageLoadingPreference)) {
            // Disable image loading
            htmlContent = htmlContent.replaceAll("<img[^>]*>", "<p>[Image removed]</p>");
        }

        // Sanitize problematic URLs and HTML structures
        htmlContent = sanitizeHtml(htmlContent);

        // Get theme-appropriate styles
        String styles = generateEmailContentStyles(isDarkTheme);

        // Add custom styling to preserve email layout but remove unwanted borders
        styles += "\n/* Fix for marketing email formatting */\n" +
                 "table { border-collapse: collapse; max-width: 100%; }\n" +
                 "table, tr, td, th { border: none !important; }\n" +
                 "td, th { padding: 0; }\n" +
                 ".body-table { width: 100%; }\n" +
                 "img { display: inline-block; max-width: 100%; height: auto; }\n" +
                 "/* Improve image quality */\n" +
                 "img { image-rendering: auto; -ms-interpolation-mode: bicubic; }\n" +
                 "/* Remove added borders */\n" +
                 "* { border: none !important; box-shadow: none !important; }\n" +
                 "/* Preserve original background colors */\n" +
                 "body > table { background-color: inherit !important; }\n" +
                 "td > table { background-color: inherit !important; }\n" +
                 "/* Optimize for smooth scrolling */\n" +
                 "html, body { scroll-behavior: smooth !important; }\n" +
                 "body { will-change: scroll-position; overflow-y: scroll; }\n" +
                 "* { -webkit-backface-visibility: hidden; -webkit-font-smoothing: antialiased; }\n";

        // Wrap content in custom styling with theme-appropriate colors
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<html><head>");
        contentBuilder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        contentBuilder.append("<style>");
        contentBuilder.append(styles);
        contentBuilder.append("</style>");
        contentBuilder.append("</head><body class='email-body'>");
        contentBuilder.append(htmlContent);
        contentBuilder.append("</body></html>");

        return contentBuilder.toString();
    }

    /**
     * Converts plain text content to HTML with appropriate styling.
     *
     * @param plainText The plain text content
     * @param isDarkTheme Whether dark theme is active
     * @return HTML-formatted content
     */
    public static String renderPlainTextContent(String plainText, boolean isDarkTheme) {
        if (plainText == null) return "<p>No content</p>";

        // Escape HTML special characters
        String escaped = plainText
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br>");

        // Get theme-appropriate styles for plain text
        // This method was identified as unused and commented out.
        // Let's use the implementation directly here for now,
        // or decide if this whole method is needed.
        String styles;
        if (isDarkTheme) {
            styles = "body { background-color: #2b2b2b; color: #bbbbbb; font-family: monospace; margin: 0; padding: 10px; } " +
                     "pre { white-space: pre-wrap; word-wrap: break-word; }";
        } else {
            styles = "body { background-color: #ffffff; color: #333333; font-family: monospace; margin: 0; padding: 10px; } " +
                     "pre { white-space: pre-wrap; word-wrap: break-word; }";
        }
        // String styles = ThemeManager.generatePlainTextStyles(isDarkTheme); // Original problematic call

        // Wrap in HTML with styling
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<html><head>");
        contentBuilder.append("<style>");
        contentBuilder.append(styles);
        contentBuilder.append("</style>");
        contentBuilder.append("</head><body><pre>");
        contentBuilder.append(escaped);
        contentBuilder.append("</pre></body></html>");

        return contentBuilder.toString();
    }

    /**
     * Sanitizes HTML content to prevent WebView errors.
     *
     * @param html The HTML content to sanitize
     * @return Sanitized HTML content
     */
    private static String sanitizeHtml(String html) {
        if (html == null) return "";

        try {
            // Fix malformed @import rules and font URLs
            html = html.replaceAll("@import\\s+url\\(['\"](http://['\"])?(https?://[^'\"]+)['\"]\\)", "@import url('$2')");

            // Fix malformed link tags with href attributes
            html = html.replaceAll("href=['\"]http://['\"]?(https?://[^'\"]+)['\"]", "href='$1'");

            // Remove any script tags for security
            html = html.replaceAll("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", "");

            // Remove potentially harmful attributes that can execute code
            html = html.replaceAll("(?i)\\son\\w+\\s*=\\s*['\"].*?['\"]", ""); // Remove event handlers (onclick, onload, etc.)

            // Fix common email HTML issues

            // 1. Fix unclosed tags - simple cases only
            html = fixUnclosedTags(html);

            // 2. Fix nested tables with missing end tags
            html = fixNestedTables(html);

            // 3. Fix CSS with !important declarations that might conflict with our styles
            html = handleCssImportantDeclarations(html);

            // 4. Fix character encoding issues
            html = handleCharacterEncodingIssues(html);

            // 5. Fix missing doctype and HTML structure
            if (!html.contains("<html") && !html.contains("<HTML")) {
                html = "<div>" + html + "</div>";
            }

            return html;
        } catch (Exception e) {
            logger.error("Error sanitizing HTML: {}", e.getMessage());
            // If sanitization fails, return a stripped version
            return html.replaceAll("<[^>]*>", "");
        }
    }

    /**
     * Fixes unclosed HTML tags in email content (simple cases).
     *
     * @param html The HTML content to fix
     * @return HTML content with fixed unclosed tags
     */
    private static String fixUnclosedTags(String html) {
        // Fix common unclosed tags in emails - this is a simplified approach
        // and won't catch all cases, but helps with basic issues

        // Common tags that should be self-closing but might not be in emails
        return html.replaceAll("<(br|img|hr|input|meta|link|source)([^>]*)(?<!/)>", "<$1$2/>")
                  // Remove font tags - they often cause problems and are deprecated
                  .replaceAll("(?i)</?font[^>]*>", "");
    }

    /**
     * Fixes nested table issues in email HTML.
     *
     * @param html The HTML content to fix
     * @return HTML content with fixed nested tables
     */
    private static String fixNestedTables(String html) {
        // Simple helper to balance table tags - won't catch all edge cases
        // but helps with common email template issues
        int openTables = 0;
        int closeTables = 0;

        // Count table tags
        int index = 0;
        while (index != -1) {
            index = html.indexOf("<table", index);
            if (index != -1) {
                openTables++;
                index += 6;
            }
        }

        index = 0;
        while (index != -1) {
            index = html.indexOf("</table>", index);
            if (index != -1) {
                closeTables++;
                index += 8;
            }
        }

        // Add missing closing tags if needed
        if (openTables > closeTables) {
            StringBuilder sb = new StringBuilder(html);
            for (int i = 0; i < openTables - closeTables; i++) {
                sb.append("</table>");
            }
            return sb.toString();
        }

        return html;
    }

    /**
     * Handles CSS !important declarations that might conflict with our styling.
     *
     * @param html The HTML content to fix
     * @return HTML content with !important declarations handled
     */
    private static String handleCssImportantDeclarations(String html) {
        // Simple approach to ensure our theme styling takes precedence
        // by removing !important from inline styles
        return html.replaceAll("(!important)", "");
    }

    /**
     * Fixes common character encoding issues in emails.
     *
     * @param html The HTML content to fix
     * @return HTML content with fixed character encoding issues
     */
    private static String handleCharacterEncodingIssues(String html) {
        // Fix common encoding issues in emails
        return html.replace("&nbsp;", " ")
                   .replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&apos;", "'");
    }
}

package com.privacyemail.util;

import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for WebView theme management.
 * Provides methods to apply dark theme to WebViews.
 */
public class WebViewThemeUtil {

    private static final Logger logger = LoggerFactory.getLogger(WebViewThemeUtil.class);
    private static final String WEBVIEW_DARK_CSS = "/com/privacyemail/css/webview-dark.css";

    /**
     * Apply dark theme to a WebView to preserve email formatting while using dark colors
     *
     * @param webView The WebView to apply dark theme to
     */
    public static void applyDarkTheme(WebView webView) {
        if (webView == null) {
            logger.warn("Cannot apply dark theme to null WebView");
            return;
        }

        try {
            // Set WebView background directly via JavaFX
            webView.setStyle("-fx-background-color: #2b2b2b;");

            // Apply external stylesheet for consistent styling
            WebEngine engine = webView.getEngine();
            String cssUrl = WebViewThemeUtil.class.getResource(WEBVIEW_DARK_CSS).toExternalForm();
            engine.setUserStyleSheetLocation(cssUrl);

            // Set initial content if empty
            if (engine.getDocument() == null) {
                String defaultContent =
                    "<html><body style='background:#2b2b2b;color:#bbbbbb;font-family:sans-serif;'>" +
                    "<p>Select an email to view its content.</p>" +
                    "</body></html>";
                engine.loadContent(defaultContent);
            }

            logger.info("Applied dark theme to WebView");
        } catch (Exception e) {
            logger.error("Error applying dark theme to WebView: {}", e.getMessage());
        }
    }
}

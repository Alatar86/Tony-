package com.privacyemail.util.interfaces;

import javafx.scene.web.WebView;
import java.util.function.Consumer;

/**
 * Interface for components that configure WebView instances.
 * This allows for different WebView configuration strategies.
 */
public interface WebViewConfigurer {

    /**
     * Configures a WebView instance for optimal content display.
     *
     * @param webView The WebView to configure
     * @param linkClickHandler Handler for external links (can be null)
     */
    void configureWebView(WebView webView, Consumer<String> linkClickHandler);

    /**
     * Loads content into a WebView with appropriate error handling.
     *
     * @param webView The WebView to load content into
     * @param htmlContent The HTML content to load
     */
    void loadContent(WebView webView, String htmlContent);

    /**
     * Cleans up resources associated with a WebView.
     * Should be called when the WebView is no longer needed.
     *
     * @param webView The WebView to clean up
     */
    void cleanupWebView(WebView webView);
}

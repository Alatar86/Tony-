package com.privacyemail.util;

import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;
import javafx.scene.text.FontSmoothingType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

/**
 * Helper class for configuring and managing WebView instances.
 * Provides common functionality for email content display.
 */
public class WebViewHelper {

    private static final Logger logger = LoggerFactory.getLogger(WebViewHelper.class);
    private static final String POST_LOAD_SCRIPT_PATH = "/scripts/email-post-load.js";
    private static final String WEBVIEW_DARK_CSS = "/com/privacyemail/css/webview-dark.css";

    /**
     * Configures a WebView instance for optimal email content display.
     *
     * @param webView The WebView to configure
     * @param linkClickHandler Handler for external links (can be null)
     */
    public static void configureWebView(WebView webView, Consumer<String> linkClickHandler) {
        if (webView == null) {
            logger.warn("Cannot configure null WebView");
            return;
        }

        logger.debug("Configuring WebView for email display");

        // Enable hardware acceleration for better rendering performance
        webView.setCache(true);

        // Improve text rendering quality
        webView.setFontSmoothingType(FontSmoothingType.LCD);

        // Optimize for smooth performance
        optimizeForSmoothScrolling(webView);

        // Set higher quality hints
        webView.setContextMenuEnabled(false); // Disable context menu for cleaner UI

        // Set up WebView engine
        WebEngine engine = webView.getEngine();

        // Set user agent to ensure proper rendering
        engine.setUserAgent(engine.getUserAgent() + " PrivacyEmail/1.0");

        // Enable JavaScript for better rendering control
        engine.setJavaScriptEnabled(true);

        // Prevent popups
        engine.setCreatePopupHandler(config -> null);

        // Handle content loading
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Handle successful load
                handleSuccessfulLoad(webView, linkClickHandler);
            } else if (newState == Worker.State.FAILED) {
                // Handle load failure
                handleLoadFailure(webView);
            }
        });
    }

    /**
     * Loads HTML content into the WebView with error handling.
     *
     * @param webView The WebView to load content into
     * @param htmlContent The HTML content to load
     */
    public static void loadContent(WebView webView, String htmlContent) {
        if (webView == null) {
            logger.warn("Cannot load content into null WebView");
            return;
        }

        try {
            // Clear existing content first
            webView.getEngine().loadContent("", "text/html");

            // Load the new content with proper content type
            webView.getEngine().loadContent(htmlContent, "text/html");

            // Log success
            logger.debug("Content loaded successfully into WebView");
        } catch (Exception e) {
            logger.error("Error loading content into WebView: {}", e.getMessage());
            // Load a simplified error message
            String errorHtml = "<html><body style='padding: 20px; color: red;'>" +
                              "<h3>Error Loading Content</h3>" +
                              "<p>There was a problem displaying this content: " + e.getMessage() + "</p>" +
                              "</body></html>";
            webView.getEngine().loadContent(errorHtml, "text/html");
        }
    }

    /**
     * Handles successful content loading in WebView.
     *
     * @param webView The WebView that loaded successfully
     * @param linkClickHandler Handler for external links
     */
    private static void handleSuccessfulLoad(WebView webView, Consumer<String> linkClickHandler) {
        Document doc = webView.getEngine().getDocument();
        if (doc == null) {
            logger.warn("Document is null after successful load");
            return;
        }

        // Set up link handling if a handler is provided
        if (linkClickHandler != null) {
            NodeList links = doc.getElementsByTagName("a");

            for (int i = 0; i < links.getLength(); i++) {
                HTMLAnchorElement link = (HTMLAnchorElement) links.item(i);

                // Use event listener to prevent default action and open in system browser
                ((EventTarget) link).addEventListener("click", evt -> {
                    evt.preventDefault();
                    String href = link.getHref();
                    linkClickHandler.accept(href);
                }, false);
            }
        }

        // Apply post-load JavaScript for better rendering
        applyPostLoadJavaScript(webView);
    }

    /**
     * Handles WebView content load failure.
     *
     * @param webView The WebView that failed to load
     */
    private static void handleLoadFailure(WebView webView) {
        Throwable exception = webView.getEngine().getLoadWorker().getException();
        logger.error("Error loading content in WebView: {}",
                     exception != null ? exception.getMessage() : "Unknown error");

        // Load simplified/fallback content
        String errorHtml = "<div style='color: red; padding: 20px;'>" +
                          "<h3>Error Displaying Content</h3>" +
                          "<p>The content couldn't be displayed correctly. " +
                          "This is often due to complex formatting or unsupported elements.</p>" +
                          "</div>";
        webView.getEngine().loadContent(errorHtml, "text/html");
    }

    /**
     * Applies post-load JavaScript to enhance content rendering.
     *
     * @param webView The WebView to apply JavaScript to
     */
    private static void applyPostLoadJavaScript(WebView webView) {
        try {
            // Load the JavaScript file from resources
            String script = loadScriptFromResources();
            if (script != null && !script.isEmpty()) {
                webView.getEngine().executeScript(script);
            }
        } catch (Exception e) {
            logger.warn("Failed to apply post-load JavaScript: {}", e.getMessage());
        }
    }

    /**
     * Loads the post-load JavaScript file from resources.
     *
     * @return The JavaScript code as a string
     */
    private static String loadScriptFromResources() {
        // First check if resource exists
        var resourceStream = WebViewHelper.class.getResourceAsStream(POST_LOAD_SCRIPT_PATH);
        if (resourceStream == null) {
            logger.warn("JavaScript resource not found at path: {}, using fallback implementation", POST_LOAD_SCRIPT_PATH);
            return getFallbackJavaScript();
        }

        // Resource exists, read it
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            logger.error("Error loading post-load script from resources: {}", e.getMessage());
            return getFallbackJavaScript();
        }
    }

    /**
     * Provides fallback JavaScript implementation when the resource file cannot be loaded.
     *
     * @return Fallback JavaScript code as a string
     */
    private static String getFallbackJavaScript() {
        return "// Ensure images don't overflow their containers and improve quality\n" +
               "var images = document.getElementsByTagName('img');\n" +
               "for (var i = 0; i < images.length; i++) {\n" +
               "  images[i].style.maxWidth = '100%';\n" +
               "  images[i].style.height = 'auto';\n" +
               "  // Apply high-quality image rendering\n" +
               "  images[i].style.imageRendering = 'auto';\n" +
               "  images[i].style.msInterpolationMode = 'bicubic';\n" +
               "  // Force smoother rendering\n" +
               "  images[i].style.transform = 'translateZ(0)';\n" +
               "  // Check if image has width and height attributes\n" +
               "  if (images[i].hasAttribute('width') && images[i].hasAttribute('height')) {\n" +
               "    var width = images[i].getAttribute('width');\n" +
               "    if (width && width.indexOf('%') === -1) {\n" +
               "      images[i].style.width = 'auto';\n" +
               "      images[i].style.maxWidth = width + 'px';\n" +
               "    }\n" +
               "  }\n" +
               "}\n" +
               "\n" +
               "// Fix marketing emails - remove borders from tables\n" +
               "var tables = document.getElementsByTagName('table');\n" +
               "for (var i = 0; i < tables.length; i++) {\n" +
               "  tables[i].style.border = 'none';\n" +
               "  var cells = tables[i].getElementsByTagName('td');\n" +
               "  for (var j = 0; j < cells.length; j++) {\n" +
               "    cells[j].style.border = 'none';\n" +
               "  }\n" +
               "}\n" +
               "\n" +
               "// Preserve original backgrounds\n" +
               "var elements = document.querySelectorAll('[bgcolor]');\n" +
               "for (var i = 0; i < elements.length; i++) {\n" +
               "  var bgColor = elements[i].getAttribute('bgcolor');\n" +
               "  if (bgColor) {\n" +
               "    elements[i].style.backgroundColor = bgColor;\n" +
               "  }\n" +
               "}\n" +
               "\n" +
               "// Force break long words in emails to prevent horizontal scrolling\n" +
               "document.body.style.wordWrap = 'break-word';\n" +
               "document.body.style.overflowWrap = 'break-word';\n" +
               "\n" +
               "// Remove fixed positioning which can cause overlapping content\n" +
               "var allElements = document.getElementsByTagName('*');\n" +
               "for (var i = 0; i < allElements.length; i++) {\n" +
               "  var element = allElements[i];\n" +
               "  var position = window.getComputedStyle(element).getPropertyValue('position');\n" +
               "  if (position === 'fixed') {\n" +
               "    element.style.position = 'static';\n" +
               "  }\n" +
               "}\n" +
               "\n" +
               "// Set overflow handling\n" +
               "document.body.style.overflowX = 'hidden';\n" +
               "document.body.style.overflowY = 'auto';\n" +
               "\n" +
               "// Optimize scrolling performance\n" +
               "document.body.style.willChange = 'scroll-position';\n" +
               "document.body.style.backfaceVisibility = 'hidden';\n" +
               "document.body.style.WebkitFontSmoothing = 'antialiased';\n" +
               "document.body.style.perspective = '1000px';\n" +
               "\n" +
               "// Add passive scroll event listener for better performance\n" +
               "window.addEventListener('scroll', function() {}, { passive: true });\n" +
               "\n" +
               "// Optimize large elements for scrolling\n" +
               "var largeElements = document.querySelectorAll('table, div, img');\n" +
               "for (var i = 0; i < largeElements.length; i++) {\n" +
               "  if (largeElements[i].offsetHeight > 100 || largeElements[i].offsetWidth > 100) {\n" +
               "    largeElements[i].style.willChange = 'transform';\n" +
               "    largeElements[i].style.transform = 'translateZ(0)';\n" +
               "  }\n" +
               "}\n" +
               "\n" +
               "// Improve scroll performance by pre-painting\n" +
               "setTimeout(function() {\n" +
               "  window.scrollBy(0, 1);\n" +
               "  window.scrollBy(0, -1);\n" +
               "}, 100);";
    }

    /**
     * Applies optimizations for smooth scrolling to the WebView.
     *
     * @param webView The WebView to optimize
     */
    private static void optimizeForSmoothScrolling(WebView webView) {
        // Use a high refresh rate for the WebView
        webView.setFontScale(1.0); // Ensure proper font scaling

        // Use CSS property to apply hardware acceleration
        try {
            var cssUrl = WebViewHelper.class.getResource("/css/webview-smooth-scroll.css");
            if (cssUrl != null) {
                webView.getEngine().setUserStyleSheetLocation(cssUrl.toString());
            } else {
                logger.warn("Could not find webview-smooth-scroll.css resource");
            }
        } catch (Exception e) {
            logger.error("Error setting user stylesheet: {}", e.getMessage());
        }

        // Keep the scene graph as simple as possible
        webView.setMouseTransparent(false);
        webView.setFocusTraversable(true);

        // Use proper pixel scaling
        webView.setPageFill(javafx.scene.paint.Color.TRANSPARENT);

        // Reduce DOM size by limiting max height
        webView.setPrefHeight(2000);
    }

    /**
     * Apply dark theme to a WebView
     * This is the most reliable method to ensure WebView displays with dark theme
     *
     * @param webView The WebView to configure
     */
    public static void applyDarkTheme(WebView webView) {
        if (webView == null) {
            logger.warn("Cannot apply dark theme to null WebView");
            return;
        }

        try {
            // Set background color directly on WebView
            webView.setStyle("-fx-background-color: #2b2b2b;");

            // Apply dark background CSS to WebView engine
            String darkBackgroundCSS =
                "body { background-color: #2b2b2b !important; color: #bbbbbb !important; }" +
                "html { background-color: #2b2b2b !important; }";

            WebEngine engine = webView.getEngine();
            engine.setUserStyleSheetLocation(
                WebViewHelper.class.getResource(WEBVIEW_DARK_CSS).toString()
            );

            // Also inject CSS when document loads
            engine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == Worker.State.SUCCEEDED) {
                    injectStylesheet(engine, darkBackgroundCSS);
                }
            });

            logger.info("Applied dark theme to WebView");
        } catch (Exception e) {
            logger.error("Failed to apply dark theme to WebView: {}", e.getMessage());
        }
    }

    /**
     * Inject a stylesheet into a loaded document
     *
     * @param engine The WebEngine
     * @param css The CSS to inject
     */
    private static void injectStylesheet(WebEngine engine, String css) {
        try {
            Document doc = engine.getDocument();
            if (doc != null) {
                org.w3c.dom.Element style = doc.createElement("style");
                style.setTextContent(css);
                doc.getElementsByTagName("head").item(0).appendChild(style);
            }
        } catch (Exception e) {
            logger.error("Failed to inject stylesheet: {}", e.getMessage());
        }
    }
}

package com.privacyemail.util;

import com.privacyemail.api.PluginManager;
import com.privacyemail.config.UserPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility for handling HTML content in WebView, particularly for displaying emails.
 * Implements features like controlling external image loading based on user preferences.
 *
 * @deprecated This class is being phased out in favor of EmailContentRenderer and WebViewHelper.
 *             Use EmailContentRenderer for HTML content processing and WebViewHelper for WebView configuration.
 */
@Deprecated
public class HtmlContentHandler {

    private static final Logger logger = LoggerFactory.getLogger(HtmlContentHandler.class);
    private static final String PLACEHOLDER_IMAGE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath fill='%23ccc' d='M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z'/%3E%3C/svg%3E";

    // Avoid instantiation
    private HtmlContentHandler() {
    }

    /**
     * Process HTML content before loading it into the WebView, based on user preferences.
     *
     * @param htmlContent The original HTML content
     * @param webView     The WebView component to display the content
     * @param preferences User preferences for image loading
     * @param emailId     The unique identifier of the email being processed
     * @return The processed HTML content
     */
    public static String processHtmlContent(String htmlContent, WebView webView, UserPreferences preferences, String emailId) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return htmlContent;
        }

        // Configure WebView based on preferences
        configureWebView(webView, preferences.getImageLoadingPreference());

        // Process content based on preferences
        String processedContent = htmlContent;

        if (preferences.getImageLoadingPreference().equals("BLOCK_EXTERNAL")) {
            processedContent = blockExternalImages(processedContent);
        }

        // Apply plugins if enabled
        if (preferences.isPluginsEnabled()) {
            PluginManager pluginManager = PluginManager.getInstance();
            processedContent = pluginManager.processEmailContent(processedContent, emailId);
            pluginManager.configureWebView(webView, emailId);
        }

        return processedContent;
    }

    /**
     * Configure the WebView to control image loading.
     *
     * @param webView The WebView to configure
     * @param imageLoadingPreference The image loading preference
     */
    private static void configureWebView(WebView webView, String imageLoadingPreference) {
        if (webView == null) {
            return;
        }

        WebEngine engine = webView.getEngine();
        boolean loadImages = !UserPreferences.IMAGE_LOADING_NEVER.equals(imageLoadingPreference) &&
                             !UserPreferences.IMAGE_LOADING_ASK.equals(imageLoadingPreference);

        // Configure WebEngine properties
        engine.setUserDataDirectory(new File("webViewCache"));  // Set a cache directory

        // Set WebView preferences
        engine.setJavaScriptEnabled(true);  // Enable JavaScript

        // Control image loading based on preference
        try {
            Method method = engine.getClass().getDeclaredMethod("setImageLoading", boolean.class);
            method.setAccessible(true);
            method.invoke(engine, loadImages);
            logger.debug("WebView image loading set to: {}", loadImages);
        } catch (Exception e) {
            logger.error("Failed to configure WebView image loading: {}", e.getMessage());
        }
    }

    /**
     * Add a prompt to the HTML that allows users to load images on demand
     *
     * @param htmlContent The original HTML content
     * @param webView The WebView (optional)
     * @return Modified HTML with image loading prompt
     * @deprecated This method is no longer used and will be removed in a future version.
     */
    @Deprecated
    private static String addImageLoadingPrompt(String htmlContent, WebView webView) {
        // First block the images
        String blockedHtml = blockExternalImages(htmlContent);

        try {
            // Parse the HTML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(blockedHtml)));

            // Create banner element
            Element bannerDiv = doc.createElement("div");
            bannerDiv.setAttribute("id", "image-loading-banner");
            bannerDiv.setAttribute("style", "background-color: #f8f9fa; padding: 10px; margin-bottom: 10px; border: 1px solid #dee2e6; border-radius: 4px;");

            Element messageP = doc.createElement("p");
            messageP.setAttribute("style", "margin: 0;");
            messageP.setTextContent("External images have been blocked to protect your privacy.");
            bannerDiv.appendChild(messageP);

            Element loadButton = doc.createElement("button");
            loadButton.setAttribute("id", "load-images-btn");
            loadButton.setAttribute("style", "background-color: #0d6efd; color: white; border: none; padding: 5px 10px; margin-top: 5px; border-radius: 4px; cursor: pointer;");
            loadButton.setTextContent("Load external images");

            // Add JavaScript to handle click
            if (webView != null) {
                // If we have a WebView, use the JavaScript function we defined
                loadButton.setAttribute("onclick", "loadExternalImages()");
            } else {
                // Simple alert for now (this would be replaced in a real implementation)
                loadButton.setAttribute("onclick", "alert('Image loading not implemented yet')");
            }

            bannerDiv.appendChild(loadButton);

            // Add banner to body
            NodeList bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                Element body = (Element) bodyNodes.item(0);
                body.insertBefore(bannerDiv, body.getFirstChild());
            } else {
                // If no body, create one and add the banner
                Element html = doc.getDocumentElement();
                if (html == null) {
                    html = doc.createElement("html");
                    doc.appendChild(html);
                }

                Element body = doc.createElement("body");
                body.appendChild(bannerDiv);
                html.appendChild(body);
            }

            // Convert back to string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();

        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            logger.error("Error processing HTML for image loading prompt", e);

            // Fallback if parsing fails - try simpler string-based approach
            String banner = "<div id=\"image-loading-banner\" style=\"background-color: #f8f9fa; padding: 10px; margin-bottom: 10px; border: 1px solid #dee2e6; border-radius: 4px;\">"
                          + "<p style=\"margin: 0;\">External images have been blocked to protect your privacy.</p>"
                          + "<button id=\"load-images-btn\" style=\"background-color: #0d6efd; color: white; border: none; padding: 5px 10px; margin-top: 5px; border-radius: 4px; cursor: pointer;\">"
                          + "Load external images</button>";

            if (webView != null) {
                banner += "<script>document.getElementById('load-images-btn').onclick = function() { loadExternalImages(); };</script>";
            } else {
                banner += "<script>document.getElementById('load-images-btn').onclick = function() { alert('Image loading not implemented yet'); };</script>";
            }

            banner += "</div>";

            // Attempt to find body tag to insert after
            int bodyIndex = blockedHtml.indexOf("<body");
            if (bodyIndex != -1) {
                int bodyEndIndex = blockedHtml.indexOf(">", bodyIndex);
                if (bodyEndIndex != -1) {
                    return blockedHtml.substring(0, bodyEndIndex + 1) + banner + blockedHtml.substring(bodyEndIndex + 1);
                }
            }

            // If we can't find the body tag, just prepend
            return banner + blockedHtml;
        }
    }

    /**
     * Block external images in HTML by replacing them with placeholders
     *
     * @param htmlContent The original HTML content
     * @return Modified HTML with blocked images
     */
    private static String blockExternalImages(String htmlContent) {
        try {
            // Parse the HTML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Some HTML may not be well-formed XML, so wrap it to help the parser
            String wrappedHtml = "<html>" + htmlContent + "</html>";
            Document doc = builder.parse(new InputSource(new StringReader(wrappedHtml)));

            // Find all image elements
            NodeList images = doc.getElementsByTagName("img");
            for (int i = 0; i < images.getLength(); i++) {
                Element img = (Element) images.item(i);
                if (img.hasAttribute("src")) {
                    String src = img.getAttribute("src");

                    // Only modify external URLs (not data: URLs, which are embedded)
                    if (!src.startsWith("data:")) {
                        // Store original source
                        img.setAttribute("data-original-src", src);
                        // Replace with placeholder
                        img.setAttribute("src", PLACEHOLDER_IMAGE);
                    }
                }
            }

            // Handle CSS background images (simplified - a complete solution would use CSS parsing)
            NodeList elementsWithStyle = doc.getElementsByTagName("*");
            for (int i = 0; i < elementsWithStyle.getLength(); i++) {
                Element element = (Element) elementsWithStyle.item(i);
                if (element.hasAttribute("style")) {
                    String style = element.getAttribute("style");
                    if (style.contains("background") && style.contains("url(") && !style.contains("data:")) {
                        // Store original style
                        element.setAttribute("data-original-style", style);
                        // Replace background images in style
                        String newStyle = style.replaceAll("background(-image)?\\s*:\\s*url\\(['\"]?([^'\"\\)]+)['\"]?\\)",
                                                          "background$1: none");
                        element.setAttribute("style", newStyle);
                    }
                }
            }

            // Convert back to string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");

            // Extract just the content we want (removing the wrapper)
            NodeList htmlNodes = doc.getElementsByTagName("html");
            StringWriter writer = new StringWriter();

            if (htmlNodes.getLength() > 0) {
                Element htmlElement = (Element) htmlNodes.item(0);
                NodeList children = htmlElement.getChildNodes();

                // Transform each child node separately to avoid the wrapper
                for (int i = 0; i < children.getLength(); i++) {
                    transformer.transform(new DOMSource(children.item(i)), new StreamResult(writer));
                }

                return writer.toString();
            }

            // If we can't find HTML tag, just transform the whole document
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();

        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            logger.error("Error processing HTML with DOM parser, falling back to regex-based approach", e);

            // Fallback to simple regex replacement for img tags
            AtomicReference<StringBuilder> resultRef = new AtomicReference<>(new StringBuilder(htmlContent.length()));
            StringBuilder result = resultRef.get();

            // Simple pattern-based replacement of image sources
            int currentPos = 0;
            int imgStart;

            while ((imgStart = htmlContent.indexOf("<img", currentPos)) != -1) {
                result.append(htmlContent, currentPos, imgStart);

                int imgEnd = htmlContent.indexOf(">", imgStart);
                if (imgEnd == -1) {
                    result.append(htmlContent, imgStart, htmlContent.length());
                    break;
                }

                String imgTag = htmlContent.substring(imgStart, imgEnd + 1);
                processImageTag(imgTag, result);
                currentPos = imgEnd + 1;
            }

            // Append any remaining content
            if (currentPos < htmlContent.length()) {
                result.append(htmlContent, currentPos, htmlContent.length());
            }

            return result.toString();
        }
    }

    /**
     * Helper method to process an image tag for the fallback string-based approach
     *
     * @param imgTag The image tag to process
     * @param result The buffer to append results to
     */
    private static void processImageTag(String imgTag, StringBuilder result) {
        int srcStart = imgTag.indexOf(" src=");
        if (srcStart != -1) {
            char quoteChar = imgTag.charAt(srcStart + 5);
            if (quoteChar == '"' || quoteChar == '\'') {
                int srcValueStart = srcStart + 6;
                int srcValueEnd = imgTag.indexOf(quoteChar, srcValueStart);

                if (srcValueEnd != -1) {
                    String srcValue = imgTag.substring(srcValueStart, srcValueEnd);

                    // Check if it's a data URL (embedded image) - these are safe to display
                    if (srcValue.startsWith("data:")) {
                        // Keep embedded images as-is
                        result.append(imgTag);
                    } else {
                        // For external URLs, replace with placeholder
                        String modifiedTag = imgTag.substring(0, srcValueStart) +
                                           PLACEHOLDER_IMAGE +
                                           imgTag.substring(srcValueEnd) +
                                           " data-original-src=" + quoteChar + srcValue + quoteChar;
                        result.append(modifiedTag);
                    }
                    return;
                }
            }
        }

        // Default case - no valid src attribute found or other issue
        result.append(imgTag);
    }

    /**
     * Initialize the plugin system and load available plugins.
     * This method should be called during application startup.
     */
    public static void initializePlugins() {
        logger.info("Initializing email content plugin system");
        PluginManager pluginManager = PluginManager.getInstance();

        try {
            // Discover plugins using ServiceLoader
            pluginManager.discoverPlugins();

            // Initialize discovered plugins
            pluginManager.initializePlugins();

            logger.info("Plugin system initialized with {} plugins",
                    pluginManager.getEnabledPlugins().size());
        } catch (Exception e) {
            logger.error("Failed to initialize plugin system", e);
        }
    }
}

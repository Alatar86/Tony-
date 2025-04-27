package com.privacyemail.controllers;

import com.privacyemail.api.IApiClient;
import com.privacyemail.config.Configuration;
import com.privacyemail.config.FrontendPreferences;
import com.privacyemail.services.ICredentialsService;
import com.privacyemail.services.IEmailService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.assertions.api.Assertions;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple TestFX test for JavaFX UI interactions.
 * This test demonstrates basic TestFX setup without trying to load complex FXML.
 */
@ExtendWith(ApplicationExtension.class)
public class SimpleEmailTest {

    @Mock private IApiClient mockApiClient;
    @Mock private Configuration mockConfiguration;
    @Mock private FrontendPreferences mockFrontendPreferences;
    @Mock private ICredentialsService mockCredentialsService;
    @Mock private IEmailService mockEmailService;

    private ExecutorService executorService;
    private Button testButton;
    private Label resultLabel;

    /**
     * Will be called with @Before semantics, i.e., before each test method.
     *
     * @param stage - Will be injected by TestFX
     */
    @Start
    private void start(Stage stage) {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Create a real executor service
        executorService = Executors.newFixedThreadPool(2);

        // Create a simple JavaFX UI with a button and label
        testButton = new Button("Click Me");
        resultLabel = new Label("Not Clicked");

        // Add a click handler to the button
        testButton.setOnAction(e -> {
            resultLabel.setText("Button Clicked!");
        });

        // Create a layout and scene
        VBox root = new VBox(10, testButton, resultLabel);
        root.setStyle("-fx-padding: 20px;");
        Scene scene = new Scene(root, 300, 200);

        // Set the scene and show the stage
        stage.setScene(scene);
        stage.setTitle("Simple TestFX Test");
        stage.show();
    }

    @Test
    void testButtonClick(FxRobot robot) {
        // Verify initial state
        Assertions.assertThat(resultLabel).hasText("Not Clicked");

        // Click the button using TestFX
        robot.clickOn(testButton);

        // Verify the label text changed
        Assertions.assertThat(resultLabel).hasText("Button Clicked!");
    }
}

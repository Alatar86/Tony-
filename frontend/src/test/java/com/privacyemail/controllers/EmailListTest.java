package com.privacyemail.controllers;

import com.privacyemail.api.IApiClient;
import com.privacyemail.config.Configuration;
import com.privacyemail.config.FrontendPreferences;
import com.privacyemail.models.ApiResult;
import com.privacyemail.models.EmailMetadata;
import com.privacyemail.services.ICredentialsService;
import com.privacyemail.services.IEmailService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
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
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TestFX test for email list functionality.
 * This test uses a custom simplified controller and view to test email list refresh.
 */
@ExtendWith(ApplicationExtension.class)
public class EmailListTest {

    @Mock private IApiClient mockApiClient;
    @Mock private Configuration mockConfiguration;
    @Mock private FrontendPreferences mockFrontendPreferences;
    @Mock private ICredentialsService mockCredentialsService;
    @Mock private IEmailService mockEmailService;

    private ExecutorService executorService;
    private EmailListTestController controller;

    private Button refreshButton;
    private ListView<EmailMetadata> emailListView;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;

    /**
     * Simple controller for email list testing
     */
    public static class EmailListTestController implements Initializable {

        @FXML private Button refreshButton;
        @FXML private ListView<EmailMetadata> emailListView;
        @FXML private Label statusLabel;
        @FXML private ProgressIndicator progressIndicator;

        private final IApiClient apiClient;
        private final ExecutorService executorService;
        private final ObservableList<EmailMetadata> emails = FXCollections.observableArrayList();

        public EmailListTestController(IApiClient apiClient, ExecutorService executorService) {
            this.apiClient = apiClient;
            this.executorService = executorService;
        }

        @Override
        public void initialize(URL location, ResourceBundle resources) {
            emailListView.setItems(emails);
            refreshButton.setOnAction(e -> refreshEmails());
        }

        public void refreshEmails() {
            // Show progress and disable refresh button
            javafx.application.Platform.runLater(() -> {
                progressIndicator.setVisible(true);
                refreshButton.setDisable(true);
                statusLabel.setText("Loading emails...");
            });

            // Clear the current list
            emails.clear();

            // In a real app, this would be on a background thread
            executorService.submit(() -> {
                try {
                    // Fetch emails from the API
                    ApiResult<List<EmailMetadata>> result = apiClient.getEmailList("INBOX");

                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        refreshButton.setDisable(false);

                        if (result.isSuccess()) {
                            emails.addAll(result.getData());
                            statusLabel.setText("Loaded " + emails.size() + " emails");
                        } else {
                            statusLabel.setText("Error: " + result.getError().getMessage());
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        refreshButton.setDisable(false);
                        statusLabel.setText("Error: " + ex.getMessage());
                    });
                }
            });
        }
    }

    /**
     * Will be called with @Before semantics, i.e., before each test method.
     *
     * @param stage - Will be injected by TestFX
     */
    @Start
    private void start(Stage stage) throws IOException {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Create a real executor service
        executorService = Executors.newFixedThreadPool(2);

        // Create UI components
        refreshButton = new Button("Refresh Emails");
        emailListView = new ListView<>();
        statusLabel = new Label("Ready");
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);

        // Set up the controller with mocked dependencies
        controller = new EmailListTestController(mockApiClient, executorService);

        // Create a layout
        VBox root = new VBox(10);
        root.getChildren().addAll(
            refreshButton,
            emailListView,
            statusLabel,
            progressIndicator
        );
        root.setStyle("-fx-padding: 20px;");

        // Manually inject FXML annotated fields into the controller
        controller.refreshButton = this.refreshButton;
        controller.emailListView = this.emailListView;
        controller.statusLabel = this.statusLabel;
        controller.progressIndicator = this.progressIndicator;

        // Initialize the controller
        controller.initialize(null, null);

        // Create scene and show
        Scene scene = new Scene(root, 400, 500);
        stage.setScene(scene);
        stage.setTitle("Email List Test");
        stage.show();
    }

    @Test
    void testRefreshEmailList(FxRobot robot) throws IOException, InterruptedException {
        // Mock API response
        List<EmailMetadata> mockEmails = new ArrayList<>();
        EmailMetadata email1 = new EmailMetadata();
        email1.setId("id1");
        email1.setSubject("Test Email 1");
        email1.setFromAddress("test@example.com");
        mockEmails.add(email1);

        // Configure mock
        when(mockApiClient.getEmailList(anyString())).thenReturn(ApiResult.success(mockEmails));

        // Initial assertions
        Assertions.assertThat(statusLabel).hasText("Ready");
        Assertions.assertThat(progressIndicator.isVisible()).isFalse();
        Assertions.assertThat(emailListView.getItems()).isEmpty();

        // Click refresh button
        robot.clickOn(refreshButton);

        // Wait for the UI to update - loading state
        WaitForAsyncUtils.waitForFxEvents();

        // Verify the API was called (might take a moment to execute)
        verify(mockApiClient, timeout(1000).times(1)).getEmailList(eq("INBOX"));

        // Wait for async operations to complete
        WaitForAsyncUtils.sleep(1000, TimeUnit.MILLISECONDS);
        WaitForAsyncUtils.waitForFxEvents();

        // Verify UI updates after completion
        Assertions.assertThat(progressIndicator.isVisible()).isFalse();
        Assertions.assertThat(refreshButton.isDisabled()).isFalse();
        Assertions.assertThat(statusLabel).hasText("Loaded 1 emails");

        // Verify the list view was updated
        Assertions.assertThat(emailListView.getItems().size()).isEqualTo(1);
        Assertions.assertThat(emailListView.getItems().get(0).getSubject()).isEqualTo("Test Email 1");
    }
}

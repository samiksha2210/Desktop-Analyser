import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.*;

/**
 * Main JavaFX entry point.
 * - Enables WAL mode and creates tables
 * - Inserts sample data if empty
 * - Starts TrackingService
 * - Runs a small dashboard and refreshes it periodically
 * - Stops TrackingService and UI refresher on application exit
 */
public class Main extends Application {

    private TrackingService tracker;
    private ScheduledExecutorService uiRefresher;

    private Label titleLabel;
    private Label categoriesLabel;
    private Label appsLabel;
    private Label websitesLabel;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        // ----------------------
        // 1) Initialize DB
        // ----------------------
        DatabaseHelper.enableWALMode();
        DatabaseHelper.createTables();

        // ----------------------
        // 2) Seed sample data if empty
        // ----------------------
        if (DatabaseHelper.getAllCategories().isEmpty()) {
            DatabaseHelper.insertCategory("Productive");
            DatabaseHelper.insertCategory("Distracting");

            // these methods exist in your DatabaseHelper
            DatabaseHelper.insertApplication("IntelliJ IDEA", 1);  // Productive
            DatabaseHelper.insertApplication("Google Chrome", 2);  // Distracting

            DatabaseHelper.insertWebsite("stackoverflow.com", 1);  // Productive
            DatabaseHelper.insertWebsite("youtube.com", 2);        // Distracting

            System.out.println("✅ Sample data inserted into database.");
        }

        // ----------------------
        // 3) Start background tracking
        // ----------------------
        tracker = new TrackingService();
        tracker.start();

        // ----------------------
        // 4) Build UI
        // ----------------------
        titleLabel = new Label("Activity Tracker Dashboard");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        categoriesLabel = new Label("Categories: loading...");
        appsLabel = new Label("Applications: loading...");
        websitesLabel = new Label("Websites: loading...");
        statusLabel = new Label("Status: Tracking started");

        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 16px; -fx-alignment: center-left;");
        root.getChildren().addAll(titleLabel, categoriesLabel, appsLabel, websitesLabel, statusLabel);

        Scene scene = new Scene(root, 560, 320);
        primaryStage.setTitle("Activity Tracker");
        primaryStage.setScene(scene);
        primaryStage.show();

        // ----------------------
        // 5) Refresh UI periodically
        // ----------------------
        uiRefresher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ui-refresher");
            t.setDaemon(true);
            return t;
        });

        uiRefresher.scheduleAtFixedRate(() -> {
            try {
                // Run UI updates on JavaFX thread
                Platform.runLater(this::refreshUi);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Update labels from database
     */
    private void refreshUi() {
        try {
            List<String> cats = DatabaseHelper.getAllCategories();
            List<String> apps = DatabaseHelper.getAllApplications();
            List<String> sites = DatabaseHelper.getAllWebsites();

            categoriesLabel.setText("Categories: " + (cats.isEmpty() ? "<none>" : String.join(", ", cats)));
            appsLabel.setText("Applications: " + (apps.isEmpty() ? "<none>" : String.join(", ", apps)));
            websitesLabel.setText("Websites: " + (sites.isEmpty() ? "<none>" : String.join(", ", sites)));

            statusLabel.setText("Status: Tracking (logs written to Activity_Log)");
        } catch (Exception e) {
            // Don't crash UI refresh on errors; show message instead
            categoriesLabel.setText("Categories: <error>");
            appsLabel.setText("Applications: <error>");
            websitesLabel.setText("Websites: <error>");
            statusLabel.setText("Status: Error reading DB - see console");
            e.printStackTrace();
        }
    }

    /**
     * Graceful shutdown: stop the tracking service and UI refresher.
     */
    @Override
    public void stop() throws Exception {
        System.out.println("Shutting down application...");

        // 1) Stop tracking (flushes current item)
        if (tracker != null) {
            try {
                tracker.stop();
            } catch (Exception e) {
                System.err.println("Error stopping tracker:");
                e.printStackTrace();
            }
        }

        // 2) Stop UI refresher
        if (uiRefresher != null) {
            uiRefresher.shutdown();
            try {
                if (!uiRefresher.awaitTermination(2, TimeUnit.SECONDS)) {
                    uiRefresher.shutdownNow();
                }
            } catch (InterruptedException e) {
                uiRefresher.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        super.stop();
        System.out.println("Application stopped.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

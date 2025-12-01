import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.AbstractMap;
import java.util.List;  // For seeding check
public class MainDashboard extends Application {
    private DashboardPage dashboardPage;
    private WebsitesPage websitesPage;
    private CodingGamingPage codingGamingPage;
    private InsightsPage insightsPage;
    private AppsPage appsPage;
    private VBox contentArea;
    private FocusModePage focusPage;
    private TrackingService tracker;  // Add this to start tracking
    @Override
    public void start(Stage primaryStage) {
        DatabaseHelper.enableWALMode();
        DatabaseHelper.createTables();
        NotificationHelper.initTray();
        NotificationHelper.startBlockedAppChecker(30);
        tracker = new TrackingService();
        tracker.start();
        dashboardPage = new DashboardPage();
        websitesPage = new WebsitesPage();
        codingGamingPage = new CodingGamingPage();
        insightsPage = new InsightsPage();
        appsPage = new AppsPage();
        focusPage = new FocusModePage();
        NotificationHelper.initTray();
        NotificationHelper.startBreakChecker(7200, 300);

        VBox sideMenu = new VBox(10);
        sideMenu.setPadding(new Insets(12));
        sideMenu.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc;");

        Button btnDashboard = new Button("Dashboard");
        Button btnWebsites = new Button("Websites");
        Button btnCodingGaming = new Button("Coding & Gaming");
        Button btnInsights = new Button("Insights");
        Button btnApps = new Button("Applications");
        Button btnFocusMode = new Button("Focus Mode");
        btnDashboard.setMaxWidth(Double.MAX_VALUE);
        btnWebsites.setMaxWidth(Double.MAX_VALUE);
        btnCodingGaming.setMaxWidth(Double.MAX_VALUE);
        btnInsights.setMaxWidth(Double.MAX_VALUE);
        btnApps.setMaxWidth(Double.MAX_VALUE);
        btnFocusMode.setMaxWidth(Double.MAX_VALUE);
        sideMenu.getChildren().addAll(btnDashboard, btnWebsites, btnCodingGaming, btnInsights, btnApps,btnFocusMode);

        contentArea = new VBox();
        contentArea.setPadding(new Insets(12));
        showPage(dashboardPage);

        btnDashboard.setOnAction(e -> showPage(dashboardPage));
        btnWebsites.setOnAction(e -> showPage(websitesPage));
        btnCodingGaming.setOnAction(e -> showPage(codingGamingPage));
        btnInsights.setOnAction(e -> showPage(insightsPage));
        btnApps.setOnAction(e -> showPage(appsPage));
        btnFocusMode.setOnAction(e -> showPage(focusPage));
        HBox mainLayout = new HBox();
        mainLayout.getChildren().addAll(sideMenu, contentArea);
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        Scene scene = new Scene(mainLayout, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Activity Tracker Dashboard");
        primaryStage.show();
    }

    private void showPage(Pane page) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);

        if (page instanceof WebsitesPage) ((WebsitesPage) page).refresh();
        if (page instanceof InsightsPage) ((InsightsPage) page).refresh();
        if (page instanceof AppsPage) ((AppsPage) page).refresh();
        if (page instanceof FocusModePage) ((FocusModePage) page).refresh();
    }

    @Override
    public void stop() throws Exception {
        if (tracker != null) tracker.stop();
        dashboardPage.stop();
        codingGamingPage.stop();
        NotificationHelper.stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

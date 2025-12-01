import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.List;
import java.util.concurrent.*;

public class DashboardPage extends VBox {
    private final PieChart appPie = new PieChart();
    private final PieChart sitePie = new PieChart();
    private final BarChart<String, Number> topAppsBar;
    private final Label productivityLabel = new Label("Productivity: --%");

    private final ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "dashboard-refresher");
        t.setDaemon(true);
        return t;
    });

    public DashboardPage() {
        setSpacing(12);
        setPadding(new Insets(12));

        appPie.setTitle("Today — App Usage");
        sitePie.setTitle("Today — Website Usage");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        topAppsBar = new BarChart<>(xAxis, yAxis);
        topAppsBar.setTitle("Top Apps Today");
        xAxis.setLabel("App");
        yAxis.setLabel("Seconds");

        HBox chartsRow = new HBox(12, appPie, sitePie);
        getChildren().addAll(new Label("Dashboard"), chartsRow, topAppsBar, productivityLabel);

        // start periodic refresh
        refresher.scheduleAtFixedRate(this::refreshAll, 0, 5, TimeUnit.SECONDS);
    }

    private void refreshAll() {
        try {
            List<Pair<String,Integer>> appTotals = DatabaseHelper.queryAppTotalsToday();
            List<Pair<String,Integer>> siteTotals = DatabaseHelper.querySiteTotalsToday();
            Pair<Integer,Integer> prodTotal = DatabaseHelper.queryProductiveVsTotalToday();
            int productive = prodTotal.getKey();
            int total = prodTotal.getValue();
            int score = (total == 0) ? 0 : (int)((productive * 100L) / total);

            Platform.runLater(() -> {
                // update app pie
                appPie.getData().clear();
                for (Pair<String,Integer> p : appTotals) {
                    if (p.getValue() > 0) appPie.getData().add(new PieChart.Data(p.getKey(), p.getValue()));
                }

                // update site pie
                sitePie.getData().clear();
                for (Pair<String,Integer> p : siteTotals) {
                    if (p.getValue() > 0) sitePie.getData().add(new PieChart.Data(p.getKey(), p.getValue()));
                }

                // update top apps bar (top 8)
                topAppsBar.getData().clear();
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                int added = 0;
                for (Pair<String,Integer> p : appTotals) {
                    if (added++ >= 8) break;
                    series.getData().add(new XYChart.Data<>(p.getKey(), p.getValue()));
                }
                topAppsBar.getData().add(series);

                // productivity label
                productivityLabel.setText("Productivity: " + score + "% (" + productive + "s / " + total + "s)");
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void stop() {
        refresher.shutdownNow();
    }
}

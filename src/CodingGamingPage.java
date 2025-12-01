import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.List;
import java.util.concurrent.*;

public class CodingGamingPage extends VBox {
    private final LineChart<String, Number> lineChart;
    private final ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();

    public CodingGamingPage() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        lineChart = new LineChart<>(x, y);
        lineChart.setTitle("Coding vs Gaming — Last 7 days");

        getChildren().add(lineChart);
        refresher.scheduleAtFixedRate(this::refresh, 0, 30, TimeUnit.SECONDS);
    }

    private void refresh() {
        try {
            List<Pair<String,Integer>> coding = DatabaseHelper.queryDailyCategoryTotals(1, 7); // productive = 1 (coding)
            List<Pair<String,Integer>> gaming = DatabaseHelper.queryDailyCategoryTotals(2, 7); // games = 2

            XYChart.Series<String, Number> sCoding = new XYChart.Series<>();
            sCoding.setName("Coding (category 1)");
            for (Pair<String,Integer> p : coding) sCoding.getData().add(new XYChart.Data<>(p.getKey(), p.getValue()));

            XYChart.Series<String, Number> sGaming = new XYChart.Series<>();
            sGaming.setName("Gaming (category 2)");
            for (Pair<String,Integer> p : gaming) sGaming.getData().add(new XYChart.Data<>(p.getKey(), p.getValue()));

            Platform.runLater(() -> {
                lineChart.getData().clear();
                lineChart.getData().addAll(sCoding, sGaming);
            });
        } catch (Throwable t) { t.printStackTrace(); }
    }

    public void stop() { refresher.shutdownNow(); }
}

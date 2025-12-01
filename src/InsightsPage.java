import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.time.LocalDate;

public class InsightsPage extends VBox {
    private final Label info = new Label();

    public InsightsPage() {
        setPadding(new Insets(10));
        setSpacing(8);
        Button computeNow = new Button("Compute Today's Insight");
        computeNow.setOnAction(e -> computeTodayInsight());
        getChildren().addAll(new Label("Insights"), computeNow, info);
        refresh();
    }

    private void computeTodayInsight() {
        Pair<Integer,Integer> p = DatabaseHelper.queryProductiveVsTotalToday();
        int productive = p.getKey();
        int total = p.getValue();
        int score = (total == 0) ? 0 : (int)((productive * 100L) / total);
        String notes = (score > 70) ? "Great focus today!" : (score > 40) ? "Decent" : "Try to reduce distractions";
        DatabaseHelper.insertInsight(LocalDate.now(), total, productive, score, notes);
        info.setText("Inserted insight: score=" + score + " notes=" + notes);
    }

    public void refresh() {
        // Optionally list existing insights (left as exercise)
    }
}

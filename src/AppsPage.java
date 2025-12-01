import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Label;


public class AppsPage extends VBox {
    private final TableView<AppRow> table = new TableView<>();
    private final ObservableList<AppRow> data = FXCollections.observableArrayList();

    public AppsPage() {
        setPadding(new Insets(10));
        setSpacing(8);

        Label title = new Label("Applications");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TableColumn<AppRow, String> nameCol = new TableColumn<>("App");
        nameCol.setCellValueFactory(p -> p.getValue().nameProperty());

        TableColumn<AppRow, Integer> secondsCol = new TableColumn<>("Today (s)");
        secondsCol.setCellValueFactory(p -> p.getValue().secondsProperty().asObject());

        TableColumn<AppRow, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(p -> p.getValue().categoryLabelProperty());
        categoryCol.setCellFactory(ComboBoxTableCell.forTableColumn("Productive", "Distracting", "Unknown"));
        categoryCol.setOnEditCommit(evt -> {
            AppRow row = evt.getRowValue();
            String newVal = evt.getNewValue();
            int newCat = "Productive".equals(newVal) ? 1 : ("Distracting".equals(newVal) ? 2 : 0);
            row.setCategoryLabel(newVal);
            DatabaseHelper.updateApplicationCategory(row.getAppId(), newCat);
        });

        table.getColumns().addAll(nameCol, secondsCol, categoryCol);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        getChildren().addAll(title, table);
        refresh();
    }

    public void refresh() {
        data.clear();
        List<Map.Entry<Integer, Pair<String,Integer>>> rows = DatabaseHelper.queryAppsWithTodaySeconds();
        for (Map.Entry<Integer, Pair<String,Integer>> e : rows) {
            int id = e.getKey();
            String name = e.getValue().getKey();
            int seconds = e.getValue().getValue();
            String label = "Unknown";
            try (var conn = DatabaseHelper.getConnection();
                 var ps = conn.prepareStatement("SELECT category_id FROM Applications WHERE app_id = ?")) {
                ps.setInt(1, id);
                var rs = ps.executeQuery();
                if (rs.next()) {
                    int catId = rs.getInt("category_id");
                    label = (catId == 1) ? "Productive" : (catId == 2) ? "Distracting" : "Unknown";
                }
            } catch (Exception ex) { /* ignore */ }
            data.add(new AppRow(id, name, seconds, label));
        }
        table.setItems(data);
    }

    // Inner class
    public static class AppRow {
        private final javafx.beans.property.IntegerProperty appId = new javafx.beans.property.SimpleIntegerProperty();
        private final javafx.beans.property.StringProperty name = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.IntegerProperty seconds = new javafx.beans.property.SimpleIntegerProperty();
        private final javafx.beans.property.StringProperty categoryLabel = new javafx.beans.property.SimpleStringProperty();

        public AppRow(int appId, String name, int seconds, String categoryLabel) {
            this.appId.set(appId);
            this.name.set(name);
            this.seconds.set(seconds);
            this.categoryLabel.set(categoryLabel);
        }

        public int getAppId() { return appId.get(); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.IntegerProperty secondsProperty() { return seconds; }
        public javafx.beans.property.StringProperty categoryLabelProperty() { return categoryLabel; }
        public void setCategoryLabel(String value) { categoryLabel.set(value); }
    }
}

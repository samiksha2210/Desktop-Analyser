import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.List;
import java.util.Map;

public class WebsitesPage extends VBox {
    private final TableView<SiteRow> table = new TableView<>();
    private final ObservableList<SiteRow> data = FXCollections.observableArrayList();

    public WebsitesPage() {
        setPadding(new Insets(10));
        setSpacing(8);

        TableColumn<SiteRow, String> urlCol = new TableColumn<>("Website");
        urlCol.setCellValueFactory(p -> p.getValue().urlProperty());

        TableColumn<SiteRow, Integer> secondsCol = new TableColumn<>("Today (s)");
        secondsCol.setCellValueFactory(p -> p.getValue().secondsProperty().asObject());

        TableColumn<SiteRow, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(p -> p.getValue().categoryLabelProperty());
        categoryCol.setCellFactory(ComboBoxTableCell.forTableColumn("Productive", "Distracting", "Unknown"));
        categoryCol.setOnEditCommit(evt -> {
            SiteRow row = evt.getRowValue();
            String newVal = evt.getNewValue();
            int newCat = "Productive".equals(newVal) ? 1 : ("Distracting".equals(newVal) ? 2 : 0);
            row.setCategoryLabel(newVal);
            // persist change - update Websites.category_id
            try {
                String sql = "UPDATE Websites SET category_id = ? WHERE site_id = ?";
                try (var conn = DatabaseHelper.getConnection();
                     var ps = conn.prepareStatement(sql)) {
                    if (newCat > 0) ps.setInt(1, newCat); else ps.setNull(1, java.sql.Types.INTEGER);
                    ps.setInt(2, row.getSiteId());
                    ps.executeUpdate();
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        table.getColumns().addAll(urlCol, secondsCol, categoryCol);
        table.setEditable(true);

        getChildren().addAll(new Label("Websites"), table);
        refresh();
    }

    public void refresh() {
        data.clear();
        List<Map.Entry<Integer, Pair<String,Integer>>> rows = DatabaseHelper.querySitesWithTodaySeconds();
        for (Map.Entry<Integer, Pair<String,Integer>> e : rows) {
            int id = e.getKey();
            String url = e.getValue().getKey();
            int seconds = e.getValue().getValue();
            int catId = -1;
            try {
                String q = "SELECT category_id FROM Websites WHERE site_id = ?";
                try (var conn = DatabaseHelper.getConnection();
                     var ps = conn.prepareStatement(q)) {
                    ps.setInt(1, id);
                    var rs = ps.executeQuery();
                    if (rs.next()) catId = rs.getInt("category_id");
                }
            } catch (Exception ex) { /* ignore */ }
            String label = (catId == 1) ? "Productive" : (catId == 2) ? "Distracting" : "Unknown";
            data.add(new SiteRow(id, url, seconds, label));
        }
        table.setItems(data);
    }

    public static class SiteRow {
        private final javafx.beans.property.IntegerProperty siteId = new javafx.beans.property.SimpleIntegerProperty();
        private final javafx.beans.property.StringProperty url = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.IntegerProperty seconds = new javafx.beans.property.SimpleIntegerProperty();
        private final javafx.beans.property.StringProperty categoryLabel = new javafx.beans.property.SimpleStringProperty();

        public SiteRow(int siteId, String url, int seconds, String categoryLabel) {
            this.siteId.set(siteId);
            this.url.set(url);
            this.seconds.set(seconds);
            this.categoryLabel.set(categoryLabel);
        }

        public int getSiteId() { return siteId.get(); }
        public javafx.beans.property.IntegerProperty siteIdProperty() { return siteId; }

        public String getUrl() { return url.get(); }
        public javafx.beans.property.StringProperty urlProperty() { return url; }

        public int getSeconds() { return seconds.get(); }
        public javafx.beans.property.IntegerProperty secondsProperty() { return seconds; }

        public String getCategoryLabel() { return categoryLabel.get(); }
        public javafx.beans.property.StringProperty categoryLabelProperty() { return categoryLabel; }
        public void setCategoryLabel(String value) { categoryLabel.set(value); }
    }
}

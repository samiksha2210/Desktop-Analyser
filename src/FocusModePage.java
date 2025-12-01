import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap;

public class FocusModePage extends VBox {
    private final TableView<AppFocusRow> appsTable = new TableView<>();
    private final ObservableList<AppFocusRow> appsData = FXCollections.observableArrayList();
    private final CheckBox focusToggle = new CheckBox("Enable Focus Mode");
    private final Button saveButton = new Button("Save Changes");  // NEW: Save button for persistence

    public FocusModePage() {
        setPadding(new Insets(10));
        setSpacing(12);
        Label title = new Label("Focus Mode");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Apps Table
        TableColumn<AppFocusRow, Boolean> appsBlockedCol = new TableColumn<>("Blocked");
        appsBlockedCol.setCellValueFactory(p -> p.getValue().blockedProperty());
        appsBlockedCol.setCellFactory(tc -> new CheckBoxTableCell<>());
        appsBlockedCol.setEditable(true);
        // CHANGED: onEditCommit now only updates local row (no DB call here; defer to save)
        appsBlockedCol.setOnEditCommit(evt -> {
            System.out.println("[UI DEBUG] Apps local toggle: Row=" + evt.getRowValue() + ", NewVal=" + evt.getNewValue());
            AppFocusRow row = evt.getRowValue();
            if (row != null) {
                row.setBlocked(evt.getNewValue());
                appsTable.refresh();
            }
        });
        TableColumn<AppFocusRow, String> appsNameCol = new TableColumn<>("App Name");
        appsNameCol.setCellValueFactory(p -> p.getValue().nameProperty());
        appsTable.getColumns().addAll(appsBlockedCol, appsNameCol);
        appsTable.setEditable(true);
        appsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // NEW: Save Changes Button
        saveButton.setOnAction(e -> saveChanges());
        HBox buttonBox = new HBox(10, saveButton);
        buttonBox.setPadding(new Insets(5, 0, 0, 0));

        // Focus Mode Toggle (with delayed notification)
        focusToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            DatabaseHelper.setFocusModeEnabled(newVal);
            if (newVal) {
                // NEW: Schedule 10s delayed notification on enable
                NotificationHelper.scheduleDelayedNotification(
                        "Focus Mode Activated!",
                        "Distracting apps blocked—stay focused for productivity!",
                        10
                );
                System.out.println("[UI DEBUG] Focus Mode enabled - 10s notification scheduled");
            }
        });

        getChildren().addAll(
                title,
                new Label("Select distracting apps:"),
                appsTable,
                buttonBox,  // NEW: Add save button
                focusToggle
        );
        refresh();
    }

    // NEW: Save changes - iterate tables, update DB based on checkbox states
    private void saveChanges() {
        System.out.println("[UI DEBUG] Saving changes to DB...");
        int appsSaved = 0;

        // Save apps
        for (AppFocusRow row : appsData) {
            int id = row.getId();
            boolean shouldBlock = row.blockedProperty().get();
            boolean currentlyBlocked = DatabaseHelper.isAppBlocked(id);
            if (shouldBlock != currentlyBlocked) {
                if (shouldBlock) {
                    DatabaseHelper.blockApp(id);
                } else {
                    DatabaseHelper.unblockApp(id);
                }
                appsSaved++;
            }
        }

        System.out.println("[UI DEBUG] Saved " + appsSaved + " app changes");
        refresh();  // Reload from DB to confirm persistence
        NotificationHelper.showNotification("Changes Saved", "Focus Mode blocks updated (" + appsSaved + " changes).");
    }

    public void refresh() {
        System.out.println("[UI DEBUG] Refreshing Focus Mode - querying DB...");
        // Refresh apps
        appsData.clear();
        List<Map.Entry<Integer, Pair<String, Boolean>>> apps = DatabaseHelper.getAppsWithBlockedStatus();
        System.out.println("[UI DEBUG] Loaded " + apps.size() + " apps from DB");
        for (Map.Entry<Integer, Pair<String, Boolean>> e : apps) {
            int id = e.getKey();
            String name = e.getValue().getKey();
            boolean blocked = e.getValue().getValue();
            System.out.println("[UI DEBUG] App: id=" + id + ", name='" + name + "', blocked=" + blocked);
            appsData.add(new AppFocusRow(id, name, blocked));
        }
        appsTable.setItems(appsData);
        // Refresh toggle
        focusToggle.setSelected(DatabaseHelper.isFocusModeEnabled());
        System.out.println("[UI DEBUG] Toggle set to: " + DatabaseHelper.isFocusModeEnabled());
    }

    // Inner classes for table rows
    public static class AppFocusRow {
        private final SimpleIntegerProperty id = new SimpleIntegerProperty();
        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleBooleanProperty blocked = new SimpleBooleanProperty();

        public AppFocusRow(int id, String name, boolean blocked) {
            this.id.set(id);
            this.name.set(name);
            this.blocked.set(blocked);
        }

        public int getId() { return id.get(); }
        public SimpleStringProperty nameProperty() { return name; }
        public BooleanProperty blockedProperty() { return blocked; }
        public void setBlocked(boolean value) { blocked.set(value); }
    }
}
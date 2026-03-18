import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class AuditLogViewer {
    private final BankingService service = new BankingService();
    private final ObservableList<AuditRow> auditRows = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public void display() {
        Stage stage = new Stage();
        stage.setTitle("TrustVault - Audit Logs");
        Button backButton = UiTheme.createSecondaryButton("<- Back to Dashboard");
        backButton.setOnAction(event -> stage.close());

        HBox header = UiTheme.wrapHeaderAndLogo(
                "COMPLIANCE FEED",
                "Audit Logs",
                "Review administrative actions, entity changes, and timestamps.");

        Button refreshButton = UiTheme.createButton("Refresh Logs", "#7a7f87", "#45494f", "#f5efe5");
        Label statusLabel = UiTheme.createInfoLabel();
        refreshButton.setOnAction(event -> loadLogs(statusLabel, refreshButton));

        VBox actionCard = UiTheme.createCard(24, 14);
        actionCard.getChildren().addAll(
                UiTheme.createSectionTitle("Audit Controls"),
                UiTheme.createSupportingText("Reload the latest administrative activity whenever you need a fresh compliance snapshot."),
                new HBox(refreshButton),
                statusLabel);

        TableView<AuditRow> tableView = createTable();
        tableView.setItems(auditRows);

        VBox tableCard = UiTheme.createCard(24, 16);
        tableCard.getChildren().addAll(
                UiTheme.createSectionTitle("Audit Timeline"),
                UiTheme.createSupportingText("See who acted, what changed, which entity was affected, and when it happened."),
                tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        VBox root = new VBox(22, new HBox(backButton), header, actionCard, tableCard);
        root.setPadding(new Insets(28));
        root.setFillWidth(true);
        root.setStyle(UiTheme.pageBackground());
        ScrollPane scrollPane = UiTheme.createPageScrollPane(root);

        Scene scene = new Scene(scrollPane, 1320, 780);
        stage.setScene(scene);
        stage.show();

        loadLogs(statusLabel, refreshButton);
    }

    private TableView<AuditRow> createTable() {
        TableView<AuditRow> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        UiTheme.styleTable(tableView);
        tableView.setPlaceholder(UiTheme.createTablePlaceholder("No audit records available."));

        TableColumn<AuditRow, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("auditId"));

        TableColumn<AuditRow, String> adminColumn = new TableColumn<>("Admin");
        adminColumn.setCellValueFactory(new PropertyValueFactory<>("adminUsername"));

        TableColumn<AuditRow, String> actionColumn = new TableColumn<>("Action");
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("actionType"));

        TableColumn<AuditRow, String> entityColumn = new TableColumn<>("Entity");
        entityColumn.setCellValueFactory(new PropertyValueFactory<>("entityType"));

        TableColumn<AuditRow, String> entityIdColumn = new TableColumn<>("Entity ID");
        entityIdColumn.setCellValueFactory(new PropertyValueFactory<>("entityId"));

        TableColumn<AuditRow, String> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));

        TableColumn<AuditRow, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        tableView.getColumns().addAll(idColumn, adminColumn, actionColumn, entityColumn, entityIdColumn, detailsColumn, dateColumn);
        return tableView;
    }

    private void loadLogs(Label statusLabel, Button refreshButton) {
        refreshButton.setDisable(true);
        UiTheme.setStatus(statusLabel, "Loading audit logs...", true);

        UiAsync.run(
                () -> service.loadAuditLogs(250),
                logs -> {
                    refreshButton.setDisable(false);
                    auditRows.setAll(logs.stream().map(log -> new AuditRow(
                            log.auditId(),
                            log.adminUsername(),
                            log.actionType(),
                            log.entityType(),
                            log.entityId() == null ? "-" : log.entityId(),
                            log.details() == null ? "-" : log.details(),
                            log.createdAt().format(formatter)
                    )).toList());
                    UiTheme.setStatus(statusLabel, auditRows.size() + " audit record(s) loaded.", true);
                },
                throwable -> {
                    refreshButton.setDisable(false);
                    UiTheme.setStatus(statusLabel, "Unable to load audit logs.", false);
                });
    }

    public static class AuditRow {
        private final SimpleIntegerProperty auditId;
        private final SimpleStringProperty adminUsername;
        private final SimpleStringProperty actionType;
        private final SimpleStringProperty entityType;
        private final SimpleStringProperty entityId;
        private final SimpleStringProperty details;
        private final SimpleStringProperty createdAt;

        public AuditRow(int auditId, String adminUsername, String actionType, String entityType, String entityId, String details, String createdAt) {
            this.auditId = new SimpleIntegerProperty(auditId);
            this.adminUsername = new SimpleStringProperty(adminUsername);
            this.actionType = new SimpleStringProperty(actionType);
            this.entityType = new SimpleStringProperty(entityType);
            this.entityId = new SimpleStringProperty(entityId);
            this.details = new SimpleStringProperty(details);
            this.createdAt = new SimpleStringProperty(createdAt);
        }

        public int getAuditId() {
            return auditId.get();
        }

        public String getAdminUsername() {
            return adminUsername.get();
        }

        public String getActionType() {
            return actionType.get();
        }

        public String getEntityType() {
            return entityType.get();
        }

        public String getEntityId() {
            return entityId.get();
        }

        public String getDetails() {
            return details.get();
        }

        public String getCreatedAt() {
            return createdAt.get();
        }
    }
}

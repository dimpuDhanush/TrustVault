import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class StatementCenter {
    private final BankingService service = new BankingService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));
    private final ObservableList<StatementRow> statementRows = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private String loadedAccountNumber;

    public void display() {
        Stage stage = new Stage();
        stage.setTitle("TrustVault - Statement Center");
        Button backButton = UiTheme.createSecondaryButton("<- Back to Dashboard");
        backButton.setOnAction(event -> stage.close());

        HBox header = UiTheme.wrapHeaderAndLogo(
                "STATEMENT STUDIO",
                "Statement Center",
                "Load account transactions by date range and export CSV statements.");

        VBox filterCard = UiTheme.createCard(24, 16);
        TextField accountField = UiTheme.createTextField("Account number");
        DatePicker fromDatePicker = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker toDatePicker = new DatePicker(LocalDate.now());
        UiTheme.styleDatePicker(fromDatePicker);
        UiTheme.styleDatePicker(toDatePicker);

        Button loadButton = UiTheme.createButton("Load Statement", "#7a7f87", "#45494f", "#f5efe5");
        Button exportButton = UiTheme.createButton("Export CSV", "#a88352", "#664b2d", "#f5efe5");
        Label statusLabel = UiTheme.createInfoLabel();

        loadButton.setOnAction(event -> loadStatements(accountField, fromDatePicker, toDatePicker, loadButton, exportButton, statusLabel));
        exportButton.setOnAction(event -> exportStatements(exportButton, loadButton, statusLabel));

        HBox filters = new HBox(12, accountField, fromDatePicker, toDatePicker, loadButton, exportButton);
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(accountField, Priority.ALWAYS);

        filterCard.getChildren().addAll(
                UiTheme.createSectionTitle("Statement Filters"),
                UiTheme.createSupportingText("Choose the account and date window before loading or exporting the statement."),
                filters,
                statusLabel);

        TableView<StatementRow> tableView = createTable();
        tableView.setItems(statementRows);

        VBox tableCard = UiTheme.createCard(24, 16);
        tableCard.getChildren().addAll(
                UiTheme.createSectionTitle("Statement Rows"),
                UiTheme.createSupportingText("Loaded statement data stays visible for review before you export the CSV."),
                tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        VBox root = new VBox(22, new HBox(backButton), header, filterCard, tableCard);
        root.setPadding(new Insets(28));
        root.setFillWidth(true);
        root.setStyle(UiTheme.pageBackground());
        ScrollPane scrollPane = UiTheme.createPageScrollPane(root);

        Scene scene = new Scene(scrollPane, 1240, 780);
        stage.setScene(scene);
        stage.show();
    }

    private void loadStatements(TextField accountField, DatePicker fromDatePicker, DatePicker toDatePicker,
                                Button loadButton, Button exportButton, Label statusLabel) {
        String accountNumber = accountField.getText().trim();
        if (accountNumber.isEmpty()) {
            UiTheme.setStatus(statusLabel, "Enter an account number.", false);
            return;
        }

        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            UiTheme.setStatus(statusLabel, "Select a valid date range.", false);
            return;
        }

        loadButton.setDisable(true);
        exportButton.setDisable(true);
        UiTheme.setStatus(statusLabel, "Loading statement...", true);

        UiAsync.run(
                () -> service.loadStatementEntries(accountNumber, fromDate, toDate),
                entries -> {
                    loadButton.setDisable(false);
                    exportButton.setDisable(false);
                    loadedAccountNumber = accountNumber;
                    statementRows.setAll(entries.stream().map(entry -> new StatementRow(
                            entry.transactionId(),
                            entry.transactionType(),
                            entry.amount(),
                            entry.balanceAfter(),
                            entry.note() == null ? "-" : entry.note(),
                            entry.createdAt().format(dateTimeFormatter)
                    )).toList());
                    UiTheme.setStatus(statusLabel, entries.size() + " statement row(s) loaded.", true);
                },
                throwable -> {
                    loadButton.setDisable(false);
                    exportButton.setDisable(false);
                    UiTheme.setStatus(statusLabel, "Unable to load statement.", false);
                });
    }

    private void exportStatements(Button exportButton, Button loadButton, Label statusLabel) {
        if (loadedAccountNumber == null || loadedAccountNumber.isBlank() || statementRows.isEmpty()) {
            UiTheme.setStatus(statusLabel, "Load a statement first.", false);
            return;
        }

        exportButton.setDisable(true);
        loadButton.setDisable(true);
        UiTheme.setStatus(statusLabel, "Exporting CSV...", true);

        List<StatementRow> rowsToExport = List.copyOf(statementRows);
        String accountNumber = loadedAccountNumber;
        UiAsync.run(
                () -> {
                    Files.createDirectories(Path.of("exports"));
                    String fileName = "statement-" + accountNumber + "-" + System.currentTimeMillis() + ".csv";
                    Path outputFile = Path.of("exports", fileName);

                    StringBuilder csvBuilder = new StringBuilder("Transaction ID,Type,Amount,Balance After,Note,Date\n");
                    for (StatementRow row : rowsToExport) {
                        csvBuilder.append(row.getTransactionId()).append(',')
                                .append(row.getTransactionType()).append(',')
                                .append(row.getAmount()).append(',')
                                .append(row.getBalanceAfter()).append(',')
                                .append('"').append(row.getNote().replace("\"", "\"\"")).append('"').append(',')
                                .append('"').append(row.getCreatedAt()).append('"').append('\n');
                    }

                    Files.writeString(outputFile, csvBuilder.toString());
                    return outputFile;
                },
                outputFile -> {
                    exportButton.setDisable(false);
                    loadButton.setDisable(false);
                    UiTheme.setStatus(statusLabel, "Statement exported to " + outputFile, true);
                },
                throwable -> {
                    exportButton.setDisable(false);
                    loadButton.setDisable(false);
                    UiTheme.setStatus(statusLabel, "Unable to export statement.", false);
                });
    }

    private TableView<StatementRow> createTable() {
        TableView<StatementRow> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        UiTheme.styleTable(tableView);
        tableView.setPlaceholder(UiTheme.createTablePlaceholder("No statement rows available."));

        TableColumn<StatementRow, Integer> idColumn = new TableColumn<>("Txn ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("transactionId"));

        TableColumn<StatementRow, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("transactionType"));

        TableColumn<StatementRow, BigDecimal> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setCellFactory(column -> new MoneyCell());

        TableColumn<StatementRow, BigDecimal> balanceColumn = new TableColumn<>("Balance After");
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        balanceColumn.setCellFactory(column -> new MoneyCell());

        TableColumn<StatementRow, String> noteColumn = new TableColumn<>("Note");
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        TableColumn<StatementRow, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        tableView.getColumns().addAll(idColumn, typeColumn, amountColumn, balanceColumn, noteColumn, dateColumn);
        return tableView;
    }

    public static class StatementRow {
        private final SimpleIntegerProperty transactionId;
        private final SimpleStringProperty transactionType;
        private final SimpleObjectProperty<BigDecimal> amount;
        private final SimpleObjectProperty<BigDecimal> balanceAfter;
        private final SimpleStringProperty note;
        private final SimpleStringProperty createdAt;

        public StatementRow(int transactionId, String transactionType, BigDecimal amount, BigDecimal balanceAfter, String note, String createdAt) {
            this.transactionId = new SimpleIntegerProperty(transactionId);
            this.transactionType = new SimpleStringProperty(transactionType);
            this.amount = new SimpleObjectProperty<>(amount);
            this.balanceAfter = new SimpleObjectProperty<>(balanceAfter);
            this.note = new SimpleStringProperty(note);
            this.createdAt = new SimpleStringProperty(createdAt);
        }

        public int getTransactionId() {
            return transactionId.get();
        }

        public String getTransactionType() {
            return transactionType.get();
        }

        public BigDecimal getAmount() {
            return amount.get();
        }

        public BigDecimal getBalanceAfter() {
            return balanceAfter.get();
        }

        public String getNote() {
            return note.get();
        }

        public String getCreatedAt() {
            return createdAt.get();
        }
    }

    private class MoneyCell extends TableCell<StatementRow, BigDecimal> {
        @Override
        protected void updateItem(BigDecimal item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(currencyFormat.format(item));
                setTextFill(Color.WHITE);
            }
        }
    }
}

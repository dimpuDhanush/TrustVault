import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TransferCenter {
    private final BankingService service = new BankingService();
    private final BankingRepository repository = new BankingRepository();
    private final ObservableList<BeneficiaryRow> beneficiaryRows = FXCollections.observableArrayList();
    private final ObservableList<TransferRow> transferRows = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private final String currentAdmin;

    public TransferCenter(String currentAdmin) {
        this.currentAdmin = currentAdmin;
    }

    public void display() {
        Stage stage = new Stage();
        stage.setTitle("TrustVault - Transfer Center");
        Button backButton = UiTheme.createSecondaryButton("<- Back to Dashboard");
        backButton.setOnAction(event -> stage.close());

        HBox header = UiTheme.wrapHeaderAndLogo(
                "TRANSFER RAIL",
                "Transfer Center",
                "Manage beneficiaries and process transfers between active accounts.");

        HBox signals = new HBox(14,
                createSignalCard("Beneficiaries", "Saved payees are visible alongside transfer history."),
                createSignalCard("Controls", "Source and target accounts stay explicit during transfer entry."));
        signals.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        VBox beneficiaryCard = createBeneficiaryCard();
        VBox transferCard = createTransferCard();
        HBox forms = new HBox(18, beneficiaryCard, transferCard);
        HBox.setHgrow(beneficiaryCard, Priority.ALWAYS);
        HBox.setHgrow(transferCard, Priority.ALWAYS);

        TableView<BeneficiaryRow> beneficiaryTable = createBeneficiaryTable();
        TableView<TransferRow> transferTable = createTransferTable();
        beneficiaryTable.setItems(beneficiaryRows);
        transferTable.setItems(transferRows);

        VBox beneficiaryTableCard = createTableCard("Saved Beneficiaries",
                "Review trusted payees before initiating a transfer.",
                beneficiaryTable);
        VBox transferTableCard = createTableCard("Recent Transfers",
                "Keep the latest money movement visible while processing new entries.",
                transferTable);
        HBox tables = new HBox(18, beneficiaryTableCard, transferTableCard);
        HBox.setHgrow(beneficiaryTableCard, Priority.ALWAYS);
        HBox.setHgrow(transferTableCard, Priority.ALWAYS);

        VBox root = new VBox(22, new HBox(backButton), header, signals, forms, tables);
        root.setPadding(new Insets(28));
        root.setFillWidth(true);
        root.setStyle(UiTheme.pageBackground());
        ScrollPane scrollPane = UiTheme.createPageScrollPane(root);

        Scene scene = new Scene(scrollPane, 1420, 860);
        stage.setScene(scene);
        stage.show();

        refreshTables();
    }

    private VBox createSignalCard(String titleText, String bodyText) {
        VBox card = UiTheme.createSoftPanel();
        Label title = new Label(titleText);
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 17));
        title.setStyle("-fx-text-fill: #f4f7fb;");
        card.getChildren().addAll(title, UiTheme.createSupportingText(bodyText));
        return card;
    }

    private VBox createBeneficiaryCard() {
        VBox card = UiTheme.createCard(24, 16);

        ComboBox<Dashboard.CustomerOption> customerCombo = UiTheme.createComboBox("Select customer");
        loadCustomerOptions(customerCombo);
        TextField nicknameField = UiTheme.createTextField("Nickname");
        TextField beneficiaryNameField = UiTheme.createTextField("Beneficiary name");
        TextField accountField = UiTheme.createTextField("Beneficiary account number");
        TextField bankField = UiTheme.createTextField("Bank name");
        TextField ifscField = UiTheme.createTextField("IFSC code");
        Label statusLabel = UiTheme.createInfoLabel();

        Button addButton = UiTheme.createButton("Save Beneficiary", "#7a7f87", "#45494f", "#f5efe5");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(event -> {
            Dashboard.CustomerOption customer = customerCombo.getValue();
            if (customer == null || nicknameField.getText().isBlank() || beneficiaryNameField.getText().isBlank() || accountField.getText().isBlank()) {
                UiTheme.setStatus(statusLabel, "Fill customer, nickname, beneficiary name, and account number.", false);
                return;
            }

            addButton.setDisable(true);
            UiTheme.setStatus(statusLabel, "Saving beneficiary...", true);

            UiAsync.run(
                    () -> service.addBeneficiary(customer.customerId(), nicknameField.getText().trim(),
                            beneficiaryNameField.getText().trim(), accountField.getText().trim(),
                            bankField.getText().trim(), ifscField.getText().trim(), currentAdmin),
                    result -> {
                        addButton.setDisable(false);
                        boolean success = result.startsWith("Beneficiary added");
                        UiTheme.setStatus(statusLabel, result, success);

                        if (success) {
                            customerCombo.getSelectionModel().clearSelection();
                            nicknameField.clear();
                            beneficiaryNameField.clear();
                            accountField.clear();
                            bankField.clear();
                            ifscField.clear();
                            refreshTables();
                        }
                    },
                    throwable -> {
                        addButton.setDisable(false);
                        UiTheme.setStatus(statusLabel, "Unable to add beneficiary.", false);
                    });
        });

        card.getChildren().addAll(
                UiTheme.createSectionTitle("Add Beneficiary"),
                UiTheme.createSupportingText("Store trusted payout details once so repeat transfers stay fast and accurate."),
                customerCombo,
                nicknameField,
                beneficiaryNameField,
                accountField,
                bankField,
                ifscField,
                addButton,
                statusLabel);
        return card;
    }

    private VBox createTransferCard() {
        VBox card = UiTheme.createCard(24, 16);

        TextField fromAccountField = UiTheme.createTextField("From account number");
        TextField toAccountField = UiTheme.createTextField("To account number");
        TextField amountField = UiTheme.createTextField("Amount");
        TextField noteField = UiTheme.createTextField("Note / description");
        Label statusLabel = UiTheme.createInfoLabel();

        Button transferButton = UiTheme.createButton("Process Transfer", "#a88352", "#664b2d", "#f5efe5");
        transferButton.setMaxWidth(Double.MAX_VALUE);
        transferButton.setOnAction(event -> {
            BigDecimal amount = parseAmount(amountField.getText().trim());
            if (fromAccountField.getText().isBlank() || toAccountField.getText().isBlank() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                UiTheme.setStatus(statusLabel, "Enter valid source, target, and amount.", false);
                return;
            }

            transferButton.setDisable(true);
            UiTheme.setStatus(statusLabel, "Processing transfer...", true);

            UiAsync.run(
                    () -> service.transfer(fromAccountField.getText().trim(), toAccountField.getText().trim(), amount,
                            noteField.getText().trim(), currentAdmin),
                    result -> {
                        transferButton.setDisable(false);
                        boolean success = result.startsWith("Transfer successful");
                        UiTheme.setStatus(statusLabel, result, success);

                        if (success) {
                            amountField.clear();
                            noteField.clear();
                            refreshTables();
                        }
                    },
                    throwable -> {
                        transferButton.setDisable(false);
                        UiTheme.setStatus(statusLabel, "Unable to process transfer.", false);
                    });
        });

        card.getChildren().addAll(
                UiTheme.createSectionTitle("Transfer Funds"),
                UiTheme.createSupportingText("Move money between active accounts with clear source, destination, and note fields."),
                fromAccountField,
                toAccountField,
                amountField,
                noteField,
                transferButton,
                statusLabel);
        return card;
    }

    private VBox createTableCard(String title, String subtitle, TableView<?> tableView) {
        VBox card = UiTheme.createCard(24, 16);
        card.getChildren().addAll(
                UiTheme.createSectionTitle(title),
                UiTheme.createSupportingText(subtitle),
                tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        return card;
    }

    private TableView<BeneficiaryRow> createBeneficiaryTable() {
        TableView<BeneficiaryRow> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        UiTheme.styleTable(tableView);
        tableView.setPlaceholder(UiTheme.createTablePlaceholder("No beneficiaries available."));

        TableColumn<BeneficiaryRow, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("beneficiaryId"));

        TableColumn<BeneficiaryRow, String> ownerColumn = new TableColumn<>("Owner");
        ownerColumn.setCellValueFactory(new PropertyValueFactory<>("ownerName"));

        TableColumn<BeneficiaryRow, String> nicknameColumn = new TableColumn<>("Nickname");
        nicknameColumn.setCellValueFactory(new PropertyValueFactory<>("nickname"));

        TableColumn<BeneficiaryRow, String> nameColumn = new TableColumn<>("Beneficiary");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("beneficiaryName"));

        TableColumn<BeneficiaryRow, String> accountColumn = new TableColumn<>("Account");
        accountColumn.setCellValueFactory(new PropertyValueFactory<>("beneficiaryAccountNumber"));

        tableView.getColumns().addAll(idColumn, ownerColumn, nicknameColumn, nameColumn, accountColumn);
        return tableView;
    }

    private TableView<TransferRow> createTransferTable() {
        TableView<TransferRow> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        UiTheme.styleTable(tableView);
        tableView.setPlaceholder(UiTheme.createTablePlaceholder("No transfers available."));

        TableColumn<TransferRow, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("transferId"));

        TableColumn<TransferRow, String> fromColumn = new TableColumn<>("From");
        fromColumn.setCellValueFactory(new PropertyValueFactory<>("fromSummary"));

        TableColumn<TransferRow, String> toColumn = new TableColumn<>("To");
        toColumn.setCellValueFactory(new PropertyValueFactory<>("toSummary"));

        TableColumn<TransferRow, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<TransferRow, String> adminColumn = new TableColumn<>("By");
        adminColumn.setCellValueFactory(new PropertyValueFactory<>("createdBy"));

        TableColumn<TransferRow, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        tableView.getColumns().addAll(idColumn, fromColumn, toColumn, amountColumn, adminColumn, dateColumn);
        return tableView;
    }

    private void refreshTables() {
        UiAsync.run(
                () -> new TransferSnapshot(service.loadBeneficiaries(), service.loadRecentTransfers()),
                snapshot -> {
                    beneficiaryRows.setAll(snapshot.beneficiaries().stream().map(item -> new BeneficiaryRow(
                            item.beneficiaryId(),
                            item.ownerName(),
                            item.nickname(),
                            item.beneficiaryName(),
                            item.beneficiaryAccountNumber()
                    )).toList());

                    transferRows.setAll(snapshot.transfers().stream().map(item -> new TransferRow(
                            item.transferId(),
                            item.fromCustomerName() + " | " + item.fromAccountNumber(),
                            item.toCustomerName() + " | " + item.toAccountNumber(),
                            currencyFormat.format(item.amount()),
                            item.createdBy(),
                            item.createdAt().format(formatter)
                    )).toList());
                },
                throwable -> {
                });
    }

    private void loadCustomerOptions(ComboBox<Dashboard.CustomerOption> customerCombo) {
        customerCombo.setDisable(true);
        UiAsync.run(
                repository::loadCustomerOptions,
                options -> {
                    customerCombo.setDisable(false);
                    customerCombo.setItems(FXCollections.observableArrayList(options));
                },
                throwable -> customerCombo.setDisable(false));
    }

    private BigDecimal parseAmount(String input) {
        try {
            return new BigDecimal(input);
        } catch (Exception exception) {
            return null;
        }
    }

    public static class BeneficiaryRow {
        private final SimpleIntegerProperty beneficiaryId;
        private final SimpleStringProperty ownerName;
        private final SimpleStringProperty nickname;
        private final SimpleStringProperty beneficiaryName;
        private final SimpleStringProperty beneficiaryAccountNumber;

        public BeneficiaryRow(int beneficiaryId, String ownerName, String nickname, String beneficiaryName, String beneficiaryAccountNumber) {
            this.beneficiaryId = new SimpleIntegerProperty(beneficiaryId);
            this.ownerName = new SimpleStringProperty(ownerName);
            this.nickname = new SimpleStringProperty(nickname);
            this.beneficiaryName = new SimpleStringProperty(beneficiaryName);
            this.beneficiaryAccountNumber = new SimpleStringProperty(beneficiaryAccountNumber);
        }

        public int getBeneficiaryId() {
            return beneficiaryId.get();
        }

        public String getOwnerName() {
            return ownerName.get();
        }

        public String getNickname() {
            return nickname.get();
        }

        public String getBeneficiaryName() {
            return beneficiaryName.get();
        }

        public String getBeneficiaryAccountNumber() {
            return beneficiaryAccountNumber.get();
        }
    }

    public static class TransferRow {
        private final SimpleIntegerProperty transferId;
        private final SimpleStringProperty fromSummary;
        private final SimpleStringProperty toSummary;
        private final SimpleStringProperty amount;
        private final SimpleStringProperty createdBy;
        private final SimpleStringProperty createdAt;

        public TransferRow(int transferId, String fromSummary, String toSummary, String amount, String createdBy, String createdAt) {
            this.transferId = new SimpleIntegerProperty(transferId);
            this.fromSummary = new SimpleStringProperty(fromSummary);
            this.toSummary = new SimpleStringProperty(toSummary);
            this.amount = new SimpleStringProperty(amount);
            this.createdBy = new SimpleStringProperty(createdBy);
            this.createdAt = new SimpleStringProperty(createdAt);
        }

        public int getTransferId() {
            return transferId.get();
        }

        public String getFromSummary() {
            return fromSummary.get();
        }

        public String getToSummary() {
            return toSummary.get();
        }

        public String getAmount() {
            return amount.get();
        }

        public String getCreatedBy() {
            return createdBy.get();
        }

        public String getCreatedAt() {
            return createdAt.get();
        }
    }

    private record TransferSnapshot(java.util.List<BankingService.BeneficiaryData> beneficiaries,
                                    java.util.List<BankingService.TransferData> transfers) {
    }
}

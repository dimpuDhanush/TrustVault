import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CreditDesk {
    private final BankingService service = new BankingService();
    private final BankingRepository repository = new BankingRepository();
    private final ObservableList<LoanRow> loanRows = FXCollections.observableArrayList();
    private final ObservableList<FixedDepositRow> fdRows = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final String currentAdmin;

    public CreditDesk(String currentAdmin) {
        this.currentAdmin = currentAdmin;
    }

    public void display() {
        Stage stage = new Stage();
        stage.setTitle("TrustVault - Loans & Deposits");
        Button backButton = UiTheme.createSecondaryButton("<- Back to Dashboard");
        backButton.setOnAction(event -> stage.close());

        HBox header = UiTheme.wrapHeaderAndLogo(
                "LENDING",
                "Loans & Deposits",
                "Issue loans, record repayments, and create fixed deposits.");

        HBox signalRow = new HBox(14,
                createSignalCard("Loan Desk", "Issue credit products with structured rate and tenure input."),
                createSignalCard("Repayments", "Capture loan payments without leaving the screen."));
        signalRow.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        HBox forms = new HBox(18, createLoanCard(), createLoanPaymentCard(), createFdCard());
        forms.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        TableView<LoanRow> loanTable = createLoanTable();
        loanTable.setItems(loanRows);
        TableView<FixedDepositRow> fdTable = createFdTable();
        fdTable.setItems(fdRows);

        VBox loanTableCard = createTableCard("Loans Portfolio",
                "Monitor loan value, EMI, payout, and repayment status from one grid.",
                loanTable);
        VBox fdTableCard = createTableCard("Fixed Deposits",
                "Review maturity values and maturity dates for fixed deposits.",
                fdTable);
        HBox tables = new HBox(18, loanTableCard, fdTableCard);
        HBox.setHgrow(loanTableCard, Priority.ALWAYS);
        HBox.setHgrow(fdTableCard, Priority.ALWAYS);

        VBox root = new VBox(22, new HBox(backButton), header, signalRow, forms, tables);
        root.setPadding(new Insets(28));
        root.setFillWidth(true);
        root.setStyle(UiTheme.pageBackground());
        ScrollPane scrollPane = UiTheme.createPageScrollPane(root);

        Scene scene = new Scene(scrollPane, 1520, 920);
        stage.setScene(scene);
        stage.show();

        refreshTables();
    }

    private VBox createSignalCard(String titleText, String bodyText) {
        VBox card = UiTheme.createSoftPanel();
        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: #f4f7fb; -fx-font-size: 17px; -fx-font-weight: bold;");
        card.getChildren().addAll(title, UiTheme.createSupportingText(bodyText));
        return card;
    }

    private VBox createLoanCard() {
        VBox card = UiTheme.createCard(24, 16);

        ComboBox<Dashboard.CustomerOption> customerCombo = UiTheme.createComboBox("Select customer");
        loadCustomerOptions(customerCombo);

        ComboBox<String> loanTypeCombo = UiTheme.createComboBox("Loan type");
        loanTypeCombo.setItems(FXCollections.observableArrayList("Personal", "Home", "Vehicle", "Business"));

        TextField principalField = UiTheme.createTextField("Principal amount");
        TextField rateField = UiTheme.createTextField("Annual interest rate");
        TextField tenureField = UiTheme.createTextField("Tenure in months");
        Label statusLabel = UiTheme.createInfoLabel();

        Button issueButton = UiTheme.createButton("Issue Loan", "#7a7f87", "#45494f", "#f5efe5");
        issueButton.setMaxWidth(Double.MAX_VALUE);
        issueButton.setOnAction(event -> {
            Dashboard.CustomerOption customer = customerCombo.getValue();
            BigDecimal principal = parseAmount(principalField.getText().trim());
            BigDecimal rate = parseAmount(rateField.getText().trim());
            Integer tenure = parseInteger(tenureField.getText().trim());

            if (customer == null || loanTypeCombo.getValue() == null || principal == null || rate == null || tenure == null || principal.compareTo(BigDecimal.ZERO) <= 0 || tenure <= 0) {
                UiTheme.setStatus(statusLabel, "Enter valid loan data.", false);
                return;
            }

            issueButton.setDisable(true);
            UiTheme.setStatus(statusLabel, "Issuing loan...", true);

            UiAsync.run(
                    () -> service.issueLoan(customer.customerId(), loanTypeCombo.getValue(), principal, rate, tenure, currentAdmin),
                    result -> {
                        issueButton.setDisable(false);
                        boolean success = result.startsWith("Loan issued");
                        UiTheme.setStatus(statusLabel, result, success);

                        if (success) {
                            customerCombo.getSelectionModel().clearSelection();
                            loanTypeCombo.getSelectionModel().clearSelection();
                            principalField.clear();
                            rateField.clear();
                            tenureField.clear();
                            refreshTables();
                        }
                    },
                    throwable -> {
                        issueButton.setDisable(false);
                        UiTheme.setStatus(statusLabel, "Unable to issue loan.", false);
                    });
        });

        card.getChildren().addAll(
                UiTheme.createSectionTitle("Issue Loan"),
                UiTheme.createSupportingText("Create a new loan with customer, product type, pricing, and tenure."),
                customerCombo,
                loanTypeCombo,
                principalField,
                rateField,
                tenureField,
                issueButton,
                statusLabel);
        return card;
    }

    private VBox createLoanPaymentCard() {
        VBox card = UiTheme.createCard(24, 16);

        TextField loanIdField = UiTheme.createTextField("Loan ID");
        TextField amountField = UiTheme.createTextField("Payment amount");
        TextField noteField = UiTheme.createTextField("Payment note");
        Label statusLabel = UiTheme.createInfoLabel();

        Button paymentButton = UiTheme.createButton("Record Payment", "#a88352", "#664b2d", "#f5efe5");
        paymentButton.setMaxWidth(Double.MAX_VALUE);
        paymentButton.setOnAction(event -> {
            Integer loanId = parseInteger(loanIdField.getText().trim());
            BigDecimal amount = parseAmount(amountField.getText().trim());
            if (loanId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                UiTheme.setStatus(statusLabel, "Enter a valid loan ID and amount.", false);
                return;
            }

            paymentButton.setDisable(true);
            UiTheme.setStatus(statusLabel, "Recording payment...", true);

            UiAsync.run(
                    () -> service.recordLoanPayment(loanId, amount, noteField.getText().trim(), currentAdmin),
                    result -> {
                        paymentButton.setDisable(false);
                        boolean success = result.startsWith("Loan payment recorded");
                        UiTheme.setStatus(statusLabel, result, success);

                        if (success) {
                            amountField.clear();
                            noteField.clear();
                            refreshTables();
                        }
                    },
                    throwable -> {
                        paymentButton.setDisable(false);
                        UiTheme.setStatus(statusLabel, "Unable to record loan payment.", false);
                    });
        });

        card.getChildren().addAll(
                UiTheme.createSectionTitle("Loan Payment"),
                UiTheme.createSupportingText("Apply repayments with a loan identifier, amount, and optional payment note."),
                loanIdField,
                amountField,
                noteField,
                paymentButton,
                statusLabel);
        return card;
    }

    private VBox createFdCard() {
        VBox card = UiTheme.createCard(24, 16);

        ComboBox<Dashboard.CustomerOption> customerCombo = UiTheme.createComboBox("Select customer");
        loadCustomerOptions(customerCombo);

        TextField principalField = UiTheme.createTextField("Principal amount");
        TextField rateField = UiTheme.createTextField("Annual interest rate");
        TextField tenureField = UiTheme.createTextField("Tenure in months");
        Label statusLabel = UiTheme.createInfoLabel();

        Button createButton = UiTheme.createButton("Create FD", "#b18b59", "#6d5332", "#f5efe5");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> {
            Dashboard.CustomerOption customer = customerCombo.getValue();
            BigDecimal principal = parseAmount(principalField.getText().trim());
            BigDecimal rate = parseAmount(rateField.getText().trim());
            Integer tenure = parseInteger(tenureField.getText().trim());

            if (customer == null || principal == null || rate == null || tenure == null || principal.compareTo(BigDecimal.ZERO) <= 0 || tenure <= 0) {
                UiTheme.setStatus(statusLabel, "Enter valid fixed deposit data.", false);
                return;
            }

            createButton.setDisable(true);
            UiTheme.setStatus(statusLabel, "Creating fixed deposit...", true);

            UiAsync.run(
                    () -> service.createFixedDeposit(customer.customerId(), principal, rate, tenure, currentAdmin),
                    result -> {
                        createButton.setDisable(false);
                        boolean success = result.startsWith("Fixed deposit created");
                        UiTheme.setStatus(statusLabel, result, success);

                        if (success) {
                            customerCombo.getSelectionModel().clearSelection();
                            principalField.clear();
                            rateField.clear();
                            tenureField.clear();
                            refreshTables();
                        }
                    },
                    throwable -> {
                        createButton.setDisable(false);
                        UiTheme.setStatus(statusLabel, "Unable to create fixed deposit.", false);
                    });
        });

        card.getChildren().addAll(
                UiTheme.createSectionTitle("Fixed Deposit"),
                UiTheme.createSupportingText("Create a fixed deposit with principal, rate, and tenure in one workflow."),
                customerCombo,
                principalField,
                rateField,
                tenureField,
                createButton,
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

    private TableView<LoanRow> createLoanTable() {
        TableView<LoanRow> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        UiTheme.styleTable(tableView);
        tableView.setPlaceholder(UiTheme.createTablePlaceholder("No loans available."));

        TableColumn<LoanRow, Integer> idColumn = new TableColumn<>("Loan ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("loanId"));

        TableColumn<LoanRow, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<LoanRow, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("loanType"));

        TableColumn<LoanRow, BigDecimal> emiColumn = new TableColumn<>("EMI");
        emiColumn.setCellValueFactory(new PropertyValueFactory<>("monthlyEmi"));
        emiColumn.setCellFactory(column -> new MoneyCell<>());

        TableColumn<LoanRow, BigDecimal> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalPayable"));
        totalColumn.setCellFactory(column -> new MoneyCell<>());

        TableColumn<LoanRow, BigDecimal> paidColumn = new TableColumn<>("Paid");
        paidColumn.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        paidColumn.setCellFactory(column -> new MoneyCell<>());

        TableColumn<LoanRow, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableView.getColumns().addAll(idColumn, customerColumn, typeColumn, emiColumn, totalColumn, paidColumn, statusColumn);
        return tableView;
    }

    private TableView<FixedDepositRow> createFdTable() {
        TableView<FixedDepositRow> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        UiTheme.styleTable(tableView);
        tableView.setPlaceholder(UiTheme.createTablePlaceholder("No fixed deposits available."));

        TableColumn<FixedDepositRow, Integer> idColumn = new TableColumn<>("FD ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("fdId"));

        TableColumn<FixedDepositRow, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<FixedDepositRow, BigDecimal> principalColumn = new TableColumn<>("Principal");
        principalColumn.setCellValueFactory(new PropertyValueFactory<>("principalAmount"));
        principalColumn.setCellFactory(column -> new MoneyCell<>());

        TableColumn<FixedDepositRow, BigDecimal> maturityColumn = new TableColumn<>("Maturity");
        maturityColumn.setCellValueFactory(new PropertyValueFactory<>("maturityAmount"));
        maturityColumn.setCellFactory(column -> new MoneyCell<>());

        TableColumn<FixedDepositRow, String> dateColumn = new TableColumn<>("Maturity Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("maturityDate"));

        TableColumn<FixedDepositRow, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableView.getColumns().addAll(idColumn, customerColumn, principalColumn, maturityColumn, dateColumn, statusColumn);
        return tableView;
    }

    private void refreshTables() {
        UiAsync.run(
                () -> new CreditSnapshot(service.loadLoans(), service.loadFixedDeposits()),
                snapshot -> {
                    loanRows.setAll(snapshot.loans().stream().map(loan -> new LoanRow(
                            loan.loanId(),
                            loan.customerName(),
                            loan.loanType(),
                            loan.monthlyEmi(),
                            loan.totalPayable(),
                            loan.paidAmount(),
                            loan.status()
                    )).toList());

                    fdRows.setAll(snapshot.fixedDeposits().stream().map(fd -> new FixedDepositRow(
                            fd.fdId(),
                            fd.customerName(),
                            fd.principalAmount(),
                            fd.maturityAmount(),
                            fd.maturityDate().format(formatter),
                            fd.status()
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

    private Integer parseInteger(String input) {
        try {
            return Integer.parseInt(input);
        } catch (Exception exception) {
            return null;
        }
    }

    public static class LoanRow {
        private final SimpleIntegerProperty loanId;
        private final SimpleStringProperty customerName;
        private final SimpleStringProperty loanType;
        private final SimpleObjectProperty<BigDecimal> monthlyEmi;
        private final SimpleObjectProperty<BigDecimal> totalPayable;
        private final SimpleObjectProperty<BigDecimal> paidAmount;
        private final SimpleStringProperty status;

        public LoanRow(int loanId, String customerName, String loanType, BigDecimal monthlyEmi, BigDecimal totalPayable, BigDecimal paidAmount, String status) {
            this.loanId = new SimpleIntegerProperty(loanId);
            this.customerName = new SimpleStringProperty(customerName);
            this.loanType = new SimpleStringProperty(loanType);
            this.monthlyEmi = new SimpleObjectProperty<>(monthlyEmi);
            this.totalPayable = new SimpleObjectProperty<>(totalPayable);
            this.paidAmount = new SimpleObjectProperty<>(paidAmount);
            this.status = new SimpleStringProperty(status);
        }

        public int getLoanId() {
            return loanId.get();
        }

        public String getCustomerName() {
            return customerName.get();
        }

        public String getLoanType() {
            return loanType.get();
        }

        public BigDecimal getMonthlyEmi() {
            return monthlyEmi.get();
        }

        public BigDecimal getTotalPayable() {
            return totalPayable.get();
        }

        public BigDecimal getPaidAmount() {
            return paidAmount.get();
        }

        public String getStatus() {
            return status.get();
        }
    }

    public static class FixedDepositRow {
        private final SimpleIntegerProperty fdId;
        private final SimpleStringProperty customerName;
        private final SimpleObjectProperty<BigDecimal> principalAmount;
        private final SimpleObjectProperty<BigDecimal> maturityAmount;
        private final SimpleStringProperty maturityDate;
        private final SimpleStringProperty status;

        public FixedDepositRow(int fdId, String customerName, BigDecimal principalAmount, BigDecimal maturityAmount, String maturityDate, String status) {
            this.fdId = new SimpleIntegerProperty(fdId);
            this.customerName = new SimpleStringProperty(customerName);
            this.principalAmount = new SimpleObjectProperty<>(principalAmount);
            this.maturityAmount = new SimpleObjectProperty<>(maturityAmount);
            this.maturityDate = new SimpleStringProperty(maturityDate);
            this.status = new SimpleStringProperty(status);
        }

        public int getFdId() {
            return fdId.get();
        }

        public String getCustomerName() {
            return customerName.get();
        }

        public BigDecimal getPrincipalAmount() {
            return principalAmount.get();
        }

        public BigDecimal getMaturityAmount() {
            return maturityAmount.get();
        }

        public String getMaturityDate() {
            return maturityDate.get();
        }

        public String getStatus() {
            return status.get();
        }
    }

    private class MoneyCell<T> extends TableCell<T, BigDecimal> {
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

    private record CreditSnapshot(java.util.List<BankingService.LoanData> loans,
                                  java.util.List<BankingService.FixedDepositData> fixedDeposits) {
    }
}

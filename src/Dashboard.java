import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Dashboard {
    private final BankingRepository repository = new BankingRepository();
    private final BankingService service = new BankingService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));
    private final ObservableList<CustomerRow> customerRows = FXCollections.observableArrayList();
    private final ObservableList<AccountRow> accountRows = FXCollections.observableArrayList();
    private final ObservableList<TransactionRow> transactionRows = FXCollections.observableArrayList();
    private List<CustomerRow> cachedCustomerRows = List.of();
    private List<AccountRow> cachedAccountRows = List.of();
    private final BorderPane root = new BorderPane();
    private final VBox contentHolder = new VBox();
    private final Map<String, Button> navigationButtons = new LinkedHashMap<>();
    private final Deque<String> navigationHistory = new ArrayDeque<>();
    private final String currentAdmin;
    private final String currentRole;
    private String currentSection = "overview";

    private Label customersMetric;
    private Label accountsMetric;
    private Label balanceMetric;
    private Label transactionsMetric;
    private ComboBox<CustomerOption> customerComboBox;
    private ComboBox<String> accountTypeComboBox;
    private TextField initialDepositField;
    private TextField customerNameField;
    private TextField customerEmailField;
    private TextField customerPhoneField;
    private TextArea customerAddressArea;
    private TextField customerSearchField;
    private TextField accountSearchField;
    private TextField transactionAccountField;
    private TextField transactionAmountField;
    private TextField transactionNoteField;
    private Label selectedAccountLabel;

    public Dashboard() {
        this("SYSTEM", "ADMIN");
    }

    public Dashboard(String currentAdmin, String currentRole) {
        this.currentAdmin = currentAdmin;
        this.currentRole = currentRole == null || currentRole.isBlank() ? "ADMIN" : currentRole;
    }

    public void display(Stage stage) {
        stage.setTitle("TrustVault - Banking Operations");

        contentHolder.setSpacing(22);
        contentHolder.setPadding(new Insets(28));
        contentHolder.setFillWidth(true);
        contentHolder.setStyle(UiTheme.pageBackground());
        ScrollPane contentScroll = UiTheme.createPageScrollPane(contentHolder);

        root.setLeft(createSidebar(stage));
        root.setCenter(contentScroll);
        root.setStyle(UiTheme.pageBackground());

        Scene scene = new Scene(root, 1480, 900);
        stage.setScene(scene);

        navigateTo("overview", false);
        stage.show();
    }

    private VBox createSidebar(Stage stage) {
        VBox sidebar = new VBox(18);
        sidebar.setPadding(new Insets(24));
        sidebar.setPrefWidth(280);
        sidebar.setStyle(UiTheme.sidebarBackground()
                + "-fx-border-color: rgba(191,154,103,0.14); -fx-border-width: 0 1 0 0;");

        Label brand = new Label("TrustVault");
        brand.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        brand.setStyle("-fx-text-fill: #f4f7fb;");

        Label subtitle = new Label("Banking operations");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #b4aa9b; -fx-font-size: 13px;");

        VBox brandBlock = new VBox(10,
                UiTheme.createLogo(108, 108),
                brand,
                subtitle);
        brandBlock.setAlignment(Pos.CENTER_LEFT);

        Label navHeader = new Label("Operations");
        navHeader.setStyle("-fx-text-fill: #8b8175; -fx-font-size: 12px; -fx-font-weight: bold;");

        Button overviewButton = UiTheme.createNavButton("Overview");
        Button customersButton = UiTheme.createNavButton("Customers");
        Button accountsButton = UiTheme.createNavButton("Accounts");
        Button transactionsButton = UiTheme.createNavButton("Transactions");
        Button transferButton = UiTheme.createNavButton("Transfers");
        Button statementButton = UiTheme.createNavButton("Statements");
        Button creditDeskButton = UiTheme.createNavButton("Loans & Deposits");
        Button auditButton = UiTheme.createNavButton("Audit Logs");

        registerNavButton("overview", overviewButton);
        registerNavButton("customers", customersButton);
        registerNavButton("accounts", accountsButton);
        registerNavButton("transactions", transactionsButton);
        transferButton.setOnAction(event -> openTransferCenter());
        statementButton.setOnAction(event -> openStatementCenter());
        creditDeskButton.setOnAction(event -> openLoansAndDeposits());
        auditButton.setOnAction(event -> openAuditLogs());

        boolean adminPrivileges = hasAdminPrivileges();
        creditDeskButton.setDisable(!adminPrivileges);
        auditButton.setDisable(!adminPrivileges);

        VBox navGroup = new VBox(10,
                navHeader,
                overviewButton,
                customersButton,
                accountsButton,
                transactionsButton,
                transferButton,
                statementButton,
                creditDeskButton,
                auditButton);
        navGroup.setFillWidth(true);

        VBox sidebarContent = new VBox(18, brandBlock, navGroup);
        sidebarContent.setFillWidth(true);

        ScrollPane menuScroll = new ScrollPane(sidebarContent);
        menuScroll.setFitToWidth(true);
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        menuScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        menuScroll.setPannable(true);
        menuScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; "
                + "-fx-background-insets: 0; -fx-padding: 0;");
        VBox.setVgrow(menuScroll, Priority.ALWAYS);

        Button logoutButton = UiTheme.createButton("Logout", "#c9515c", "#8d2233", "#fff6f5");
        logoutButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setOnAction(event -> new Login().show(stage));

        sidebar.getChildren().addAll(menuScroll, logoutButton);
        return sidebar;
    }

    private void registerNavButton(String key, Button button) {
        navigationButtons.put(key, button);
        button.setOnAction(event -> navigateTo(key));
    }

    private void setActiveNav(String key) {
        navigationButtons.forEach((name, button) -> UiTheme.setNavButtonActive(button, name.equals(key)));
    }

    private void navigateTo(String sectionKey) {
        navigateTo(sectionKey, true);
    }

    private void navigateTo(String sectionKey, boolean rememberCurrent) {
        if (rememberCurrent && currentSection != null && !currentSection.equals(sectionKey)) {
            navigationHistory.push(currentSection);
        }
        currentSection = sectionKey;
        setActiveNav(sectionKey);
        switch (sectionKey) {
            case "customers" -> showCustomers();
            case "accounts" -> showAccounts();
            case "transactions" -> showTransactions();
            default -> showOverview();
        }
    }

    private void goBack() {
        if (navigationHistory.isEmpty()) {
            navigateTo("overview", false);
            return;
        }
        navigateTo(navigationHistory.pop(), false);
    }

    private void showOverview() {
        setActiveNav("overview");

        BankingRepository.OverviewStats stats = new BankingRepository.OverviewStats(0, 0, BigDecimal.ZERO, 0);
        VBox page = createPageShell(
                "OVERVIEW",
                "Banking Overview",
                "Monitor customer volume, account volume, total balances, and recent transactions.");

        VBox customerMetricCard = UiTheme.createMetricCard("Customers", String.valueOf(stats.customerCount()),
                "Customer profiles currently available for account and transaction workflows.", "#a89a84");
        VBox accountMetricCard = UiTheme.createMetricCard("Accounts", String.valueOf(stats.accountCount()),
                "Savings, current, and deposit products under active management.", "#8f8578");
        VBox balanceMetricCard = UiTheme.createMetricCard("Total Balance", currencyFormat.format(stats.totalBalance()),
                "Combined live balance across active customer accounts.", "#c4965a");
        VBox transactionMetricCard = UiTheme.createMetricCard("Transactions", String.valueOf(stats.transactionCount()),
                "Transactions recorded in the system.", "#7c6b56");

        HBox metrics = new HBox(16, customerMetricCard, accountMetricCard, balanceMetricCard, transactionMetricCard);
        customersMetric = (Label) customerMetricCard.getChildren().get(1);
        accountsMetric = (Label) accountMetricCard.getChildren().get(1);
        balanceMetric = (Label) balanceMetricCard.getChildren().get(1);
        transactionsMetric = (Label) transactionMetricCard.getChildren().get(1);

        VBox operationsCard = UiTheme.createCard(24, 18);
        operationsCard.setPrefWidth(560);
        operationsCard.getChildren().addAll(
                UiTheme.createSectionTitle("Operational Scope"),
                UiTheme.createSupportingText("Use the left menu to open each operation. This overview summarizes the banking work handled in the system."),
                createOverviewSummaryPanel("Customer intake",
                        "Customer onboarding, contact updates, and directory review."),
                createOverviewSummaryPanel("Account controls",
                        "Account opening, lifecycle updates, and account monitoring."),
                createOverviewSummaryPanel("Cash movement",
                        "Deposits, withdrawals, beneficiary handling, and transfers."),
                createOverviewSummaryPanel("Lending and reporting",
                        "Loans, fixed deposits, statement export, and audit review."));

        TableView<TransactionRow> recentTable = createTransactionTable();
        recentTable.setItems(FXCollections.observableArrayList());

        VBox recentCard = UiTheme.createCard(24, 16);
        recentCard.getChildren().addAll(
                UiTheme.createSectionTitle("Recent Transactions"),
                UiTheme.createSupportingText("Review the latest posted transactions."),
                recentTable);
        HBox.setHgrow(recentCard, Priority.ALWAYS);

        HBox lowerSection = new HBox(18, operationsCard, recentCard);
        HBox.setHgrow(recentCard, Priority.ALWAYS);

        page.getChildren().addAll(metrics, lowerSection);
        contentHolder.getChildren().setAll(page);
        loadOverviewData(recentTable);
    }

    private void showCustomers() {
        setActiveNav("customers");

        VBox page = createPageShell(
                "CUSTOMERS",
                "Customer Management",
                "Create customer records and search existing customer data.");

        VBox formCard = createContentCard(430);
        customerNameField = UiTheme.createTextField("Full name");
        customerEmailField = UiTheme.createTextField("Email address");
        customerPhoneField = UiTheme.createTextField("Phone number");
        customerAddressArea = UiTheme.createTextArea("Address", 4);

        Button saveCustomerButton = UiTheme.createButton("Save Customer", "#a88352", "#664b2d", "#f5efe5");
        Label formStatus = UiTheme.createInfoLabel();
        saveCustomerButton.setMaxWidth(Double.MAX_VALUE);
        saveCustomerButton.setOnAction(event -> saveCustomer(saveCustomerButton, formStatus));

        formCard.getChildren().addAll(
                UiTheme.createSectionTitle("Add New Customer"),
                UiTheme.createSupportingText("Enter identity, contact, and address details in one place."),
                customerNameField,
                customerEmailField,
                customerPhoneField,
                customerAddressArea,
                saveCustomerButton,
                formStatus);

        VBox tableCard = createContentCard(-1);
        customerSearchField = UiTheme.createTextField("Search by name, email, or phone");
        customerSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyCustomerFilter());

        TableView<CustomerRow> customerTable = createCustomerTable();
        customerTable.setItems(customerRows);

        tableCard.getChildren().addAll(
                UiTheme.createSectionTitle("Customer Directory"),
                UiTheme.createSupportingText("Search by name, email, or phone."),
                customerSearchField,
                customerTable);

        HBox layout = new HBox(18, formCard, tableCard);
        HBox.setHgrow(tableCard, Priority.ALWAYS);

        page.getChildren().add(layout);
        contentHolder.getChildren().setAll(page);
        refreshCustomerTable();
    }

    private void showAccounts() {
        setActiveNav("accounts");

        VBox page = createPageShell(
                "ACCOUNTS",
                "Account Management",
                "Open accounts and update account lifecycle status.");

        VBox formCard = createContentCard(450);
        customerComboBox = UiTheme.createComboBox("Select customer");
        accountTypeComboBox = UiTheme.createComboBox("Account type");
        accountTypeComboBox.setItems(FXCollections.observableArrayList("Savings", "Current", "Fixed Deposit"));
        initialDepositField = UiTheme.createTextField("Initial deposit");

        Button createAccountButton = UiTheme.createButton("Create Account", "#7a7f87", "#45494f", "#f5efe5");
        Label formStatus = UiTheme.createInfoLabel();
        createAccountButton.setMaxWidth(Double.MAX_VALUE);
        createAccountButton.setOnAction(event -> createAccount(createAccountButton, formStatus));

        formCard.getChildren().addAll(
                UiTheme.createSectionTitle("Open New Account"),
                UiTheme.createSupportingText("Link a customer to the right banking product and start with the opening balance."),
                customerComboBox,
                accountTypeComboBox,
                initialDepositField,
                createAccountButton,
                formStatus);

        VBox tableCard = createContentCard(-1);
        accountSearchField = UiTheme.createTextField("Search by account number, customer, or type");
        accountSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyAccountFilter());

        TableView<AccountRow> accountTable = createAccountTable();
        accountTable.setItems(accountRows);

        Label accountStatusLabel = UiTheme.createInfoLabel();
        Button freezeButton = UiTheme.createButton("Freeze", "#9a7351", "#60422b", "#f5efe5");
        Button reopenButton = UiTheme.createButton("Reopen", "#a88352", "#664b2d", "#f5efe5");
        Button closeButton = UiTheme.createButton("Close", "#864950", "#54252c", "#fff4f1");
        freezeButton.setOnAction(event -> changeSelectedAccountStatus(accountTable, "FROZEN", freezeButton, accountStatusLabel));
        reopenButton.setOnAction(event -> changeSelectedAccountStatus(accountTable, "ACTIVE", reopenButton, accountStatusLabel));
        closeButton.setOnAction(event -> changeSelectedAccountStatus(accountTable, "CLOSED", closeButton, accountStatusLabel));

        HBox accountActions = new HBox(10, freezeButton, reopenButton, closeButton);

        tableCard.getChildren().addAll(
                UiTheme.createSectionTitle("Account Portfolio"),
                UiTheme.createSupportingText("Search accounts and apply status changes."),
                accountSearchField,
                accountActions,
                accountStatusLabel,
                accountTable);

        HBox layout = new HBox(18, formCard, tableCard);
        HBox.setHgrow(tableCard, Priority.ALWAYS);

        page.getChildren().add(layout);
        contentHolder.getChildren().setAll(page);

        reloadCustomerOptions();
        refreshAccountTable();
    }

    private void showTransactions() {
        setActiveNav("transactions");

        VBox page = createPageShell(
                "TRANSACTIONS",
                "Transactions",
                "Post deposits and withdrawals and review recent transaction entries.");

        VBox transactionCard = createContentCard(430);
        transactionAccountField = UiTheme.createTextField("Account number");
        transactionAmountField = UiTheme.createTextField("Amount");
        transactionNoteField = UiTheme.createTextField("Description / note");
        selectedAccountLabel = UiTheme.createInfoLabel();

        Button findAccountButton = UiTheme.createButton("Find Account", "#7a7f87", "#45494f", "#f5efe5");
        Button depositButton = UiTheme.createButton("Deposit", "#a88352", "#664b2d", "#f5efe5");
        Button withdrawButton = UiTheme.createButton("Withdraw", "#8d6b45", "#574026", "#f5efe5");
        Label transactionStatus = UiTheme.createInfoLabel();

        findAccountButton.setOnAction(event -> lookupAccount(selectedAccountLabel, findAccountButton));
        depositButton.setOnAction(event -> processTransaction(true, depositButton, transactionStatus));
        withdrawButton.setOnAction(event -> processTransaction(false, withdrawButton, transactionStatus));

        HBox actions = new HBox(10, findAccountButton, depositButton, withdrawButton);

        transactionCard.getChildren().addAll(
                UiTheme.createSectionTitle("Post Transaction"),
                UiTheme.createSupportingText("Verify the account first, then post the amount with a clear transaction note."),
                transactionAccountField,
                transactionAmountField,
                transactionNoteField,
                selectedAccountLabel,
                actions,
                transactionStatus);

        VBox historyCard = createContentCard(-1);
        TableView<TransactionRow> transactionTable = createTransactionTable();
        transactionTable.setItems(transactionRows);

        historyCard.getChildren().addAll(
                UiTheme.createSectionTitle("Transaction History"),
                UiTheme.createSupportingText("Review the latest transaction entries."),
                transactionTable);

        HBox layout = new HBox(18, transactionCard, historyCard);
        HBox.setHgrow(historyCard, Priority.ALWAYS);

        page.getChildren().add(layout);
        contentHolder.getChildren().setAll(page);
        refreshTransactionTable();
    }

    private VBox createPageShell(String eyebrow, String title, String subtitle) {
        VBox page = new VBox(22);
        if (!navigationHistory.isEmpty()) {
            Button backButton = UiTheme.createSecondaryButton("<- Back");
            backButton.setOnAction(event -> goBack());
            page.getChildren().add(backButton);
        }

        HBox header = UiTheme.wrapHeaderAndLogo(eyebrow, title, subtitle);
        page.getChildren().add(header);
        return page;
    }

    private VBox createContentCard(double preferredWidth) {
        VBox card = UiTheme.createCard(24, 16);
        if (preferredWidth > 0) {
            card.setPrefWidth(preferredWidth);
        }
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox createOverviewSummaryPanel(String title, String description) {
        VBox panel = UiTheme.createSoftPanel();

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 17));
        titleLabel.setStyle("-fx-text-fill: #f1ede6;");

        panel.getChildren().addAll(
                titleLabel,
                UiTheme.createSupportingText(description));
        return panel;
    }

    private boolean hasAdminPrivileges() {
        return "ADMIN".equalsIgnoreCase(currentRole) || "MANAGER".equalsIgnoreCase(currentRole);
    }

    private void openTransferCenter() {
        new TransferCenter(currentAdmin).display();
    }

    private void openStatementCenter() {
        new StatementCenter().display();
    }

    private void openLoansAndDeposits() {
        new CreditDesk(currentAdmin).display();
    }

    private void openAuditLogs() {
        new AuditLogViewer().display();
    }

    private TableView<CustomerRow> createCustomerTable() {
        TableView<CustomerRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        UiTheme.styleTable(table);
        table.setPlaceholder(UiTheme.createTablePlaceholder("No customers available."));

        TableColumn<CustomerRow, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("customerId"));

        TableColumn<CustomerRow, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<CustomerRow, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<CustomerRow, String> phoneColumn = new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));

        table.getColumns().addAll(idColumn, nameColumn, emailColumn, phoneColumn);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private TableView<AccountRow> createAccountTable() {
        TableView<AccountRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        UiTheme.styleTable(table);
        table.setPlaceholder(UiTheme.createTablePlaceholder("No accounts available."));

        TableColumn<AccountRow, String> accountColumn = new TableColumn<>("Account Number");
        accountColumn.setCellValueFactory(new PropertyValueFactory<>("accountNumber"));

        TableColumn<AccountRow, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<AccountRow, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("accountType"));

        TableColumn<AccountRow, BigDecimal> balanceColumn = new TableColumn<>("Balance");
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        balanceColumn.setCellFactory(column -> new StyledMoneyCell<>());

        TableColumn<AccountRow, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell());

        table.getColumns().addAll(accountColumn, customerColumn, typeColumn, balanceColumn, statusColumn);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private TableView<TransactionRow> createTransactionTable() {
        TableView<TransactionRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        UiTheme.styleTable(table);
        table.setPlaceholder(UiTheme.createTablePlaceholder("No transactions available."));

        TableColumn<TransactionRow, Integer> idColumn = new TableColumn<>("Txn ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("transactionId"));

        TableColumn<TransactionRow, String> accountColumn = new TableColumn<>("Account");
        accountColumn.setCellValueFactory(new PropertyValueFactory<>("accountNumber"));

        TableColumn<TransactionRow, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<TransactionRow, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("transactionType"));
        typeColumn.setCellFactory(column -> new TransactionTypeCell());

        TableColumn<TransactionRow, BigDecimal> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setCellFactory(column -> new StyledMoneyCell<>());

        TableColumn<TransactionRow, BigDecimal> balanceColumn = new TableColumn<>("Balance After");
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        balanceColumn.setCellFactory(column -> new StyledMoneyCell<>());

        TableColumn<TransactionRow, String> timeColumn = new TableColumn<>("Date");
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        table.getColumns().addAll(idColumn, accountColumn, customerColumn, typeColumn, amountColumn, balanceColumn, timeColumn);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private void saveCustomer(Button actionButton, Label statusLabel) {
        String fullName = customerNameField.getText().trim();
        String email = customerEmailField.getText().trim();
        String phone = customerPhoneField.getText().trim();
        String address = customerAddressArea.getText().trim();

        if (fullName.isEmpty() || email.isEmpty()) {
            UiTheme.setStatus(statusLabel, "Full name and email are required.", false);
            return;
        }

        actionButton.setDisable(true);
        UiTheme.setStatus(statusLabel, "Saving customer...", true);

        UiAsync.run(
                () -> service.addCustomer(fullName, email, phone, address, currentAdmin),
                result -> {
                    actionButton.setDisable(false);
                    boolean success = result.startsWith("Customer created");
                    UiTheme.setStatus(statusLabel, result, success);

                    if (success) {
                        customerNameField.clear();
                        customerEmailField.clear();
                        customerPhoneField.clear();
                        customerAddressArea.clear();
                        refreshCustomerTable();
                        reloadCustomerOptions();
                        refreshOverviewMetrics();
                    }
                },
                throwable -> {
                    actionButton.setDisable(false);
                    UiTheme.setStatus(statusLabel, "Unable to create customer.", false);
                });
    }

    private void createAccount(Button actionButton, Label statusLabel) {
        CustomerOption customer = customerComboBox.getValue();
        String accountType = accountTypeComboBox.getValue();
        BigDecimal initialDeposit = parseAmount(initialDepositField.getText().trim());

        if (customer == null || accountType == null) {
            UiTheme.setStatus(statusLabel, "Select a customer and account type.", false);
            return;
        }

        if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            UiTheme.setStatus(statusLabel, "Enter a valid initial deposit.", false);
            return;
        }

        actionButton.setDisable(true);
        UiTheme.setStatus(statusLabel, "Creating account...", true);

        UiAsync.run(
                () -> service.createAccount(customer.customerId(), accountType, initialDeposit, currentAdmin),
                result -> {
                    actionButton.setDisable(false);
                    boolean success = result.startsWith("Account created");
                    UiTheme.setStatus(statusLabel, result, success);

                    if (success) {
                        customerComboBox.getSelectionModel().clearSelection();
                        accountTypeComboBox.getSelectionModel().clearSelection();
                        initialDepositField.clear();
                        refreshAccountTable();
                        refreshTransactionTable();
                        refreshOverviewMetrics();
                    }
                },
                throwable -> {
                    actionButton.setDisable(false);
                    UiTheme.setStatus(statusLabel, "Unable to create account.", false);
                });
    }

    private void lookupAccount(Label statusLabel, Button actionButton) {
        String accountNumber = transactionAccountField.getText().trim();
        if (accountNumber.isEmpty()) {
            UiTheme.setStatus(statusLabel, "Enter an account number first.", false);
            return;
        }

        actionButton.setDisable(true);
        UiTheme.setStatus(statusLabel, "Loading account...", true);

        UiAsync.run(
                () -> repository.findAccount(accountNumber),
                snapshot -> {
                    actionButton.setDisable(false);
                    if (snapshot == null) {
                        UiTheme.setStatus(statusLabel, "Account not found.", false);
                        return;
                    }

                    UiTheme.setStatus(statusLabel,
                            snapshot.accountNumber() + " | " + snapshot.customerName() + " | Balance " + currencyFormat.format(snapshot.balance()),
                            true);
                },
                throwable -> {
                    actionButton.setDisable(false);
                    UiTheme.setStatus(statusLabel, "Unable to load account details.", false);
                });
    }

    private void processTransaction(boolean deposit, Button actionButton, Label statusLabel) {
        String accountNumber = transactionAccountField.getText().trim();
        BigDecimal amount = parseAmount(transactionAmountField.getText().trim());
        String note = transactionNoteField.getText().trim();

        if (accountNumber.isEmpty() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            UiTheme.setStatus(statusLabel, "Enter a valid account number and amount.", false);
            return;
        }

        actionButton.setDisable(true);
        UiTheme.setStatus(statusLabel, deposit ? "Posting deposit..." : "Posting withdrawal...", true);

        UiAsync.run(
                () -> deposit
                        ? service.deposit(accountNumber, amount, note, currentAdmin)
                        : service.withdraw(accountNumber, amount, note, currentAdmin),
                result -> {
                    actionButton.setDisable(false);
                    boolean success = result.startsWith("Transaction successful");
                    UiTheme.setStatus(statusLabel, result, success);

                    if (success) {
                        transactionAmountField.clear();
                        transactionNoteField.clear();
                        refreshAccountTable();
                        refreshTransactionTable();
                        refreshOverviewMetrics();
                        if (selectedAccountLabel != null) {
                            UiTheme.setStatus(selectedAccountLabel, "Refreshing account snapshot...", true);
                            UiAsync.run(
                                    () -> repository.findAccount(accountNumber),
                                    snapshot -> {
                                        if (snapshot == null) {
                                            UiTheme.setStatus(selectedAccountLabel, "Account not found.", false);
                                            return;
                                        }
                                        UiTheme.setStatus(selectedAccountLabel,
                                                snapshot.accountNumber() + " | " + snapshot.customerName() + " | Balance " + currencyFormat.format(snapshot.balance()),
                                                true);
                                    },
                                    throwable -> UiTheme.setStatus(selectedAccountLabel, "Unable to refresh account snapshot.", false));
                        }
                    }
                },
                throwable -> {
                    actionButton.setDisable(false);
                    UiTheme.setStatus(statusLabel, "Unable to process transaction.", false);
                });
    }

    private void reloadCustomerOptions() {
        if (customerComboBox == null) {
            return;
        }
        customerComboBox.setDisable(true);
        UiAsync.run(
                repository::loadCustomerOptions,
                options -> {
                    customerComboBox.setDisable(false);
                    customerComboBox.setItems(FXCollections.observableArrayList(options));
                },
                throwable -> customerComboBox.setDisable(false));
    }

    private void changeSelectedAccountStatus(TableView<AccountRow> accountTable, String status, Button actionButton, Label statusLabel) {
        AccountRow selectedRow = accountTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            UiTheme.setStatus(statusLabel, "Select an account first.", false);
            return;
        }

        actionButton.setDisable(true);
        UiTheme.setStatus(statusLabel, "Updating account status...", true);

        UiAsync.run(
                () -> service.updateAccountStatus(selectedRow.getAccountNumber(), status, currentAdmin),
                result -> {
                    actionButton.setDisable(false);
                    boolean success = result.startsWith("Account status updated");
                    UiTheme.setStatus(statusLabel, result, success);

                    if (success) {
                        refreshAccountTable();
                        refreshOverviewMetrics();
                    }
                },
                throwable -> {
                    actionButton.setDisable(false);
                    UiTheme.setStatus(statusLabel, "Unable to update account status.", false);
                });
    }

    private void refreshCustomerTable() {
        UiAsync.run(
                () -> mapCustomerRows(repository.loadCustomers()),
                rows -> {
                    cachedCustomerRows = rows;
                    applyCustomerFilter();
                },
                throwable -> {
                });
    }

    private void refreshAccountTable() {
        UiAsync.run(
                () -> mapAccountRows(repository.loadAccounts()),
                rows -> {
                    cachedAccountRows = rows;
                    applyAccountFilter();
                },
                throwable -> {
                });
    }

    private void refreshTransactionTable() {
        UiAsync.run(
                () -> mapTransactionRows(repository.loadRecentTransactions()),
                transactionRows::setAll,
                throwable -> {
                });
    }

    private void refreshOverviewMetrics() {
        if (customersMetric == null) {
            return;
        }

        UiAsync.run(
                repository::loadOverviewStats,
                stats -> {
                    customersMetric.setText(String.valueOf(stats.customerCount()));
                    accountsMetric.setText(String.valueOf(stats.accountCount()));
                    balanceMetric.setText(currencyFormat.format(stats.totalBalance()));
                    transactionsMetric.setText(String.valueOf(stats.transactionCount()));
                },
                throwable -> {
                });
    }

    private void loadOverviewData(TableView<TransactionRow> recentTable) {
        UiAsync.run(
                () -> new OverviewSnapshot(repository.loadOverviewStats(), mapTransactionRows(repository.loadRecentTransactions())),
                snapshot -> {
                    customersMetric.setText(String.valueOf(snapshot.stats().customerCount()));
                    accountsMetric.setText(String.valueOf(snapshot.stats().accountCount()));
                    balanceMetric.setText(currencyFormat.format(snapshot.stats().totalBalance()));
                    transactionsMetric.setText(String.valueOf(snapshot.stats().transactionCount()));
                    recentTable.setItems(FXCollections.observableArrayList(snapshot.transactions()));
                },
                throwable -> {
                });
    }

    private void applyCustomerFilter() {
        String query = customerSearchField == null ? "" : customerSearchField.getText().trim().toLowerCase();
        customerRows.setAll(cachedCustomerRows.stream().filter(row ->
                matchesQuery(row.getFullName(), query)
                        || matchesQuery(row.getEmail(), query)
                        || matchesQuery(row.getPhone(), query)).toList());
    }

    private void applyAccountFilter() {
        String query = accountSearchField == null ? "" : accountSearchField.getText().trim().toLowerCase();
        accountRows.setAll(cachedAccountRows.stream().filter(row ->
                matchesQuery(row.getAccountNumber(), query)
                        || matchesQuery(row.getCustomerName(), query)
                        || matchesQuery(row.getAccountType(), query)).toList());
    }

    private List<CustomerRow> mapCustomerRows(List<BankingRepository.CustomerData> customers) {
        return customers.stream()
                .map(customer -> new CustomerRow(
                        customer.customerId(),
                        customer.fullName(),
                        customer.email(),
                        customer.phone() == null ? "-" : customer.phone()))
                .toList();
    }

    private List<AccountRow> mapAccountRows(List<BankingRepository.AccountData> accounts) {
        return accounts.stream()
                .map(account -> new AccountRow(account.accountNumber(), account.customerName(), account.accountType(), account.balance(), account.status()))
                .toList();
    }

    private List<TransactionRow> mapTransactionRows(List<BankingRepository.TransactionData> transactions) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return transactions.stream()
                .map(transaction -> new TransactionRow(
                        transaction.transactionId(),
                        transaction.accountNumber(),
                        transaction.customerName(),
                        transaction.transactionType(),
                        transaction.amount(),
                        transaction.balanceAfter(),
                        transaction.createdAt().format(formatter)))
                .toList();
    }

    private BigDecimal parseAmount(String input) {
        try {
            return new BigDecimal(input);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean matchesQuery(String value, String query) {
        return query.isEmpty() || (value != null && value.toLowerCase().contains(query));
    }

    public static class CustomerRow {
        private final SimpleIntegerProperty customerId;
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty email;
        private final SimpleStringProperty phone;

        public CustomerRow(int customerId, String fullName, String email, String phone) {
            this.customerId = new SimpleIntegerProperty(customerId);
            this.fullName = new SimpleStringProperty(fullName);
            this.email = new SimpleStringProperty(email);
            this.phone = new SimpleStringProperty(phone == null ? "-" : phone);
        }

        public int getCustomerId() {
            return customerId.get();
        }

        public String getFullName() {
            return fullName.get();
        }

        public String getEmail() {
            return email.get();
        }

        public String getPhone() {
            return phone.get();
        }
    }

    public static class AccountRow {
        private final SimpleStringProperty accountNumber;
        private final SimpleStringProperty customerName;
        private final SimpleStringProperty accountType;
        private final SimpleObjectProperty<BigDecimal> balance;
        private final SimpleStringProperty status;

        public AccountRow(String accountNumber, String customerName, String accountType, BigDecimal balance, String status) {
            this.accountNumber = new SimpleStringProperty(accountNumber);
            this.customerName = new SimpleStringProperty(customerName);
            this.accountType = new SimpleStringProperty(accountType);
            this.balance = new SimpleObjectProperty<>(balance);
            this.status = new SimpleStringProperty(status);
        }

        public String getAccountNumber() {
            return accountNumber.get();
        }

        public String getCustomerName() {
            return customerName.get();
        }

        public String getAccountType() {
            return accountType.get();
        }

        public BigDecimal getBalance() {
            return balance.get();
        }

        public String getStatus() {
            return status.get();
        }
    }

    public static class TransactionRow {
        private final SimpleIntegerProperty transactionId;
        private final SimpleStringProperty accountNumber;
        private final SimpleStringProperty customerName;
        private final SimpleStringProperty transactionType;
        private final SimpleObjectProperty<BigDecimal> amount;
        private final SimpleObjectProperty<BigDecimal> balanceAfter;
        private final SimpleStringProperty createdAt;

        public TransactionRow(int transactionId, String accountNumber, String customerName, String transactionType,
                              BigDecimal amount, BigDecimal balanceAfter, String createdAt) {
            this.transactionId = new SimpleIntegerProperty(transactionId);
            this.accountNumber = new SimpleStringProperty(accountNumber);
            this.customerName = new SimpleStringProperty(customerName);
            this.transactionType = new SimpleStringProperty(transactionType);
            this.amount = new SimpleObjectProperty<>(amount);
            this.balanceAfter = new SimpleObjectProperty<>(balanceAfter);
            this.createdAt = new SimpleStringProperty(createdAt);
        }

        public int getTransactionId() {
            return transactionId.get();
        }

        public String getAccountNumber() {
            return accountNumber.get();
        }

        public String getCustomerName() {
            return customerName.get();
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

        public String getCreatedAt() {
            return createdAt.get();
        }
    }

    private record OverviewSnapshot(BankingRepository.OverviewStats stats, List<TransactionRow> transactions) {
    }

    public record CustomerOption(int customerId, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    private class StyledMoneyCell<T> extends TableCell<T, BigDecimal> {
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

    private class StatusCell extends TableCell<AccountRow, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                return;
            }

            setText(item);
            setStyle("-fx-font-weight: bold; -fx-alignment: CENTER;");
            switch (item.toUpperCase()) {
                case "ACTIVE" -> setTextFill(Color.web("#d0b083"));
                case "FROZEN" -> setTextFill(Color.web("#a78351"));
                case "CLOSED" -> setTextFill(Color.web("#ff8f8f"));
                default -> setTextFill(Color.web("#d7e2ef"));
            }
        }
    }

    private class TransactionTypeCell extends TableCell<TransactionRow, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                return;
            }

            setText(item);
            setStyle("-fx-font-weight: bold;");
            String normalized = item.toUpperCase();
            if (normalized.contains("DEPOSIT") || normalized.contains("TRANSFER_IN")) {
                setTextFill(Color.web("#d0b083"));
            } else if (normalized.contains("WITHDRAW") || normalized.contains("TRANSFER_OUT")) {
                setTextFill(Color.web("#b78756"));
            } else {
                setTextFill(Color.web("#a9a39a"));
            }
        }
    }
}

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Login {
    private static final Logger LOGGER = AppLogger.get(Login.class);

    public void show(Stage stage) {
        stage.setTitle("TrustVault - Secure Login");

        StackPane heroPanel = createHeroPanel();
        StackPane loginCard = createLoginCard(stage);

        HBox root = new HBox(heroPanel, loginCard);
        HBox.setHgrow(heroPanel, Priority.ALWAYS);
        HBox.setHgrow(loginCard, Priority.ALWAYS);

        Scene scene = new Scene(root, 1180, 720);
        stage.setScene(scene);
        stage.show();
    }

    private StackPane createHeroPanel() {
        VBox heroBox = new VBox(26);
        heroBox.setPadding(new Insets(56, 64, 56, 64));
        heroBox.setAlignment(Pos.CENTER_LEFT);
        heroBox.setMaxWidth(760);

        HBox header = UiTheme.wrapHeaderAndLogo(
                "OPERATIONS CONSOLE",
                "TrustVault Banking Suite",
                "Manage customers, accounts, transactions, transfers, credit operations, audit trails, and statements from one banking platform.");

        VBox highlights = UiTheme.createCard(24, 14);
        Label highlightsTitle = UiTheme.createSectionTitle("What You Can Run In TrustVault");
        Label highlightOne = UiTheme.createSupportingText("Onboard customers, open accounts, and keep account lifecycle control in one place.");
        Label highlightTwo = UiTheme.createSupportingText("Process deposits, withdrawals, transfers, and repayments with live operational visibility.");
        Label highlightThree = UiTheme.createSupportingText("Review statements, audit trails, and credit activity without leaving the application.");
        highlights.getChildren().addAll(highlightsTitle, highlightOne, highlightTwo, highlightThree);

        HBox metrics = new HBox(16,
                UiTheme.createMetricCard("Customers", "Live", "Customer onboarding and directory tools ready.", "#9d907a"),
                UiTheme.createMetricCard("Security", "Protected", "Admin password hardening now supported.", "#c4965a"));

        heroBox.getChildren().addAll(header, highlights, metrics);

        StackPane heroPanel = new StackPane(heroBox);
        heroPanel.setPadding(new Insets(20));
        heroPanel.setStyle(UiTheme.pageBackground());
        return heroPanel;
    }

    private StackPane createLoginCard(Stage stage) {
        VBox shell = new VBox();
        shell.setAlignment(Pos.CENTER);
        shell.setPadding(new Insets(48));
        shell.setPrefWidth(430);
        shell.setStyle("-fx-background-color: "
                + "radial-gradient(center 20% 14%, radius 55%, rgba(191,154,103,0.14) 0%, rgba(191,154,103,0.00) 68%), "
                + "linear-gradient(to bottom, rgba(4,5,7,0.98), rgba(8,10,14,0.98)); "
                + "-fx-border-color: rgba(191,154,103,0.14); -fx-border-width: 0 0 0 1;");

        VBox formCard = UiTheme.createCard(28, 18);
        formCard.setAlignment(Pos.CENTER_LEFT);
        formCard.setMaxWidth(348);

        VBox header = UiTheme.createScreenHeader(
                "ADMIN ACCESS",
                "Sign In",
                "Enter your administrator credentials to continue into TrustVault.");

        TextField userField = UiTheme.createTextField("Username");

        var passField = UiTheme.createPasswordField("Password");

        Button loginBtn = UiTheme.createButton("Enter TrustVault", "#a88352", "#664b2d", "#f5efe5");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = UiTheme.createInfoLabel();

        loginBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                UiTheme.setStatus(statusLabel, "Enter username and password.", false);
                return;
            }

            loginBtn.setDisable(true);
            UiTheme.setStatus(statusLabel, "Signing in...", true);

            UiAsync.run(
                    () -> validateLogin(user, pass),
                    adminSession -> {
                        loginBtn.setDisable(false);
                        if (adminSession != null) {
                            new Dashboard(adminSession.username(), adminSession.role()).display(stage);
                        } else {
                            UiTheme.setStatus(statusLabel, "Invalid username or password.", false);
                        }
                    },
                    throwable -> {
                        loginBtn.setDisable(false);
                        UiTheme.setStatus(statusLabel, "Unable to sign in right now.", false);
                    });
        });

        formCard.getChildren().addAll(header, userField, passField, loginBtn, statusLabel);
        shell.getChildren().add(formCard);
        return new StackPane(shell);
    }

    private AdminSession validateLogin(String username, String password) {
        String sql = "SELECT username, password, COALESCE(role, 'ADMIN') AS role FROM admin WHERE username = ?";
        try (Connection connect = Database.connectDB()) {
            if (connect == null) {
                return null;
            }

            try (PreparedStatement prepare = connect.prepareStatement(sql)) {
                prepare.setString(1, username);
                ResultSet result = prepare.executeQuery();
                if (!result.next()) {
                    return null;
                }

                String storedPassword = result.getString("password");
                String role = result.getString("role");
                if (!AdminSecurity.verifyPassword(password, storedPassword)) {
                    return null;
                }

                if (!AdminSecurity.isHashed(storedPassword)) {
                    upgradePasswordHash(connect, username, password);
                }

                return new AdminSession(result.getString("username"), role);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login validation failed.", e);
            return null;
        }
    }

    private void upgradePasswordHash(Connection connection, String username, String rawPassword) {
        String sql = "UPDATE admin SET password = ? WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AdminSecurity.hashPassword(rawPassword));
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Password hash upgrade failed for user " + username + ".", exception);
        }
    }

    private record AdminSession(String username, String role) {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}

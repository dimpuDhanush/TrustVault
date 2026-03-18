import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.net.URL;

public final class UiTheme {
    private static final String TEXT_PRIMARY = "#f1ede6";
    private static final String TEXT_SECONDARY = "#b4aa9b";
    private static final String TEXT_MUTED = "#847b6e";
    private static final String SURFACE_BASE = "rgba(11,10,10,0.97)";
    private static final String SURFACE_RAISED = "rgba(22,19,17,0.98)";
    private static final String BORDER_SOFT = "rgba(191,154,103,0.18)";

    private UiTheme() {
    }

    public static String pageBackground() {
        return "-fx-background-color: "
                + "radial-gradient(center 80% 14%, radius 42%, rgba(196,154,100,0.14) 0%, rgba(196,154,100,0.04) 34%, rgba(196,154,100,0.00) 68%), "
                + "radial-gradient(center 18% 82%, radius 32%, rgba(138,141,149,0.10) 0%, rgba(138,141,149,0.03) 26%, rgba(138,141,149,0.00) 62%), "
                + "linear-gradient(to bottom right, #020202 0%, #060505 42%, #0d0c0c 100%);";
    }

    public static String sidebarBackground() {
        return "-fx-background-color: "
                + "radial-gradient(center 50% 0%, radius 56%, rgba(194,154,104,0.10) 0%, rgba(194,154,104,0.00) 64%), "
                + "linear-gradient(to bottom, #020202 0%, #060505 54%, #0d0c0c 100%);";
    }

    public static VBox createScreenHeader(String eyebrowText, String titleText, String subtitleText) {
        Label eyebrow = new Label(eyebrowText);
        eyebrow.setStyle("-fx-background-color: rgba(194,154,104,0.08); -fx-text-fill: #d7b079; "
                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 7 14 7 14; "
                + "-fx-background-radius: 99; -fx-border-radius: 99; -fx-border-color: rgba(194,154,104,0.28);");

        Label title = new Label(titleText);
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 34));
        title.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");

        Label subtitle = new Label(subtitleText);
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 14px;");

        VBox header = new VBox(10, eyebrow, title, subtitle);
        return header;
    }

    public static VBox createCard() {
        return createCard(22, 16);
    }

    public static VBox createCard(double padding, double spacing) {
        VBox card = new VBox(spacing);
        card.setPadding(new Insets(padding));
        card.setStyle("-fx-background-color: "
                + "radial-gradient(center 14% 8%, radius 86%, rgba(194,154,104,0.10) 0%, rgba(194,154,104,0.00) 42%), "
                + "radial-gradient(center 92% 2%, radius 72%, rgba(149,153,160,0.07) 0%, rgba(149,153,160,0.00) 38%), "
                + "linear-gradient(to bottom right, " + SURFACE_RAISED + ", " + SURFACE_BASE + "); "
                + "-fx-background-radius: 28; -fx-border-radius: 28; "
                + "-fx-border-color: " + BORDER_SOFT + "; -fx-border-width: 1;");
        card.setEffect(new DropShadow(34, Color.rgb(0, 0, 0, 0.60)));
        return card;
    }

    public static VBox createSoftPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(18));
        panel.setStyle("-fx-background-color: rgba(255,255,255,0.022); -fx-background-radius: 22; "
                + "-fx-border-color: rgba(194,154,104,0.10); -fx-border-radius: 22; "
                + "-fx-background-insets: 0; -fx-border-insets: 0;");
        return panel;
    }

    public static Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        label.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        return label;
    }

    public static Label createSupportingText(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px;");
        return label;
    }

    public static Label createInfoLabel() {
        Label label = new Label();
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px;");
        return label;
    }

    public static void setStatus(Label label, String text, boolean success) {
        label.setText(text);
        label.setStyle((success ? "-fx-text-fill: #d8b178;" : "-fx-text-fill: #db7770;") + " -fx-font-size: 13px;");
    }

    public static TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(44);
        field.setStyle(inputStyle());
        return field;
    }

    public static PasswordField createPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setPrefHeight(44);
        field.setStyle(inputStyle());
        return field;
    }

    public static TextArea createTextArea(String prompt, int rows) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(rows);
        area.setStyle("-fx-background-color: #07090d; -fx-control-inner-background: #07090d; -fx-text-fill: " + TEXT_PRIMARY + "; "
                + "-fx-prompt-text-fill: #6f675d; -fx-highlight-fill: rgba(215,176,121,0.88); -fx-highlight-text-fill: #020202; "
                + "-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: rgba(194,154,104,0.18); "
                + "-fx-padding: 10 14 10 14; -fx-font-size: 14px;");
        return area;
    }

    public static <T> ComboBox<T> createComboBox(String prompt) {
        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.setPromptText(prompt);
        styleComboBox(comboBox);
        return comboBox;
    }

    public static void styleComboBox(ComboBox<?> comboBox) {
        comboBox.setPrefHeight(44);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setStyle("-fx-background-color: #07090d; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-prompt-text-fill: #6f675d; "
                + "-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: rgba(194,154,104,0.18);");
    }

    public static void styleDatePicker(DatePicker datePicker) {
        datePicker.setPrefHeight(44);
        datePicker.setStyle("-fx-background-color: #07090d; -fx-text-fill: " + TEXT_PRIMARY + "; "
                + "-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: rgba(194,154,104,0.18);");
    }

    public static Button createButton(String text, String startColor, String endColor, String textColor) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: linear-gradient(to right, " + startColor + ", " + endColor + "); "
                + "-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-font-size: 13px; "
                + "-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: rgba(255,255,255,0.06); "
                + "-fx-padding: 12 18 12 18;");
        return button;
    }

    public static Button createSecondaryButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: rgba(255,255,255,0.025); -fx-text-fill: #ece6dc; "
                + "-fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 16; "
                + "-fx-border-radius: 16; -fx-border-color: rgba(194,154,104,0.24); "
                + "-fx-padding: 11 18 11 18;");
        return button;
    }

    public static Button createNavButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getProperties().put("tv-active", false);
        applyNavStyle(button, false);
        button.setOnMouseEntered(event -> applyNavStyle(button, true));
        button.setOnMouseExited(event -> applyNavStyle(button, false));
        return button;
    }

    public static void setNavButtonActive(Button button, boolean active) {
        button.getProperties().put("tv-active", active);
        applyNavStyle(button, false);
    }

    private static void applyNavStyle(Button button, boolean hovered) {
        boolean active = Boolean.TRUE.equals(button.getProperties().get("tv-active"));
        if (active) {
            button.setStyle("-fx-background-color: linear-gradient(to right, rgba(168,128,77,0.94), rgba(54,41,27,0.98)); "
                    + "-fx-text-fill: #f6efe5; -fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: center-left; "
                    + "-fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(216,182,132,0.24); "
                    + "-fx-padding: 14 16 14 16; -fx-pref-width: 206;");
        } else if (hovered) {
            button.setStyle("-fx-background-color: rgba(255,255,255,0.055); -fx-text-fill: #efe9df; "
                    + "-fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: center-left; "
                    + "-fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(194,154,104,0.16); "
                    + "-fx-padding: 14 16 14 16; -fx-pref-width: 206;");
        } else {
            button.setStyle("-fx-background-color: rgba(255,255,255,0.025); -fx-text-fill: #ddd6cb; "
                    + "-fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: center-left; "
                    + "-fx-background-radius: 18; -fx-padding: 14 16 14 16; -fx-pref-width: 206; "
                    + "-fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 18;");
        }
    }

    public static String tableStyle() {
        return "-fx-background-color: rgba(5,7,10,0.98); -fx-control-inner-background: rgba(5,7,10,0.98); "
                + "-fx-text-background-color: " + TEXT_PRIMARY + "; -fx-table-cell-border-color: rgba(255,255,255,0.05); "
                + "-fx-selection-bar: #9f7a4a; -fx-selection-bar-non-focused: #6b5030;";
    }

    public static void styleTable(TableView<?> tableView) {
        tableView.setStyle(tableStyle());
        tableView.setPlaceholder(createTablePlaceholder("No data available."));
        tableView.setFixedCellSize(42);
    }

    public static VBox createMetricCard(String caption, String value, String detail, String accentColor) {
        Label captionLabel = new Label(caption);
        captionLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        valueLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");

        Label detailLabel = new Label(detail);
        detailLabel.setWrapText(true);
        detailLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");

        Label marker = new Label(" ");
        marker.setMinSize(54, 4);
        marker.setMaxSize(54, 4);
        marker.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 99;");

        VBox card = createCard(18, 12);
        card.setPrefWidth(260);
        card.getChildren().addAll(captionLabel, valueLabel, marker, detailLabel);
        return card;
    }

    public static Label createChip(String text, String backgroundColor, String textColor) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + "; "
                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 7 13 7 13; "
                + "-fx-background-radius: 99; -fx-border-radius: 99; -fx-border-color: rgba(255,255,255,0.08);");
        return chip;
    }

    public static Label createTablePlaceholder(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px;");
        return label;
    }

    public static ScrollPane createPageScrollPane(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; "
                + "-fx-padding: 0; -fx-border-color: transparent;");
        return scrollPane;
    }

    public static HBox wrapHeaderAndLogo(String eyebrow, String title, String subtitle) {
        VBox header = createScreenHeader(eyebrow, title, subtitle);
        ImageView logo = createLogo(116, 116);
        HBox row = new HBox(18, header, logo);
        HBox.setHgrow(header, Priority.ALWAYS);
        return row;
    }

    public static ImageView createLogo(double fitWidth, double fitHeight) {
        URL resource = UiTheme.class.getResource("/assets/trustvault-logo.png");
        Image image = null;
        if (resource != null) {
            image = new Image(resource.toExternalForm());
        } else {
            File logoFile = new File("assets/trustvault-logo.png");
            if (logoFile.exists()) {
                image = new Image(logoFile.toURI().toString());
            }
        }

        if (image == null) {
            return new ImageView();
        }

        ImageView logoView = new ImageView(image);
        logoView.setPreserveRatio(true);
        logoView.setFitWidth(fitWidth);
        logoView.setFitHeight(fitHeight);
        logoView.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.45)));
        return logoView;
    }

    private static String inputStyle() {
        return "-fx-background-color: #07090d; -fx-control-inner-background: #07090d; -fx-text-fill: " + TEXT_PRIMARY + "; "
                + "-fx-prompt-text-fill: #6f675d; -fx-highlight-fill: rgba(215,176,121,0.88); -fx-highlight-text-fill: #020202; "
                + "-fx-background-radius: 16; -fx-border-radius: 16; "
                + "-fx-border-color: rgba(194,154,104,0.18); -fx-padding: 0 14 0 14; -fx-font-size: 14px;";
    }
}

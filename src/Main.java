import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        AppLogger.configure();
        SchemaManager.initializeDatabase();
        new Login().show(stage);
    }

    @Override
    public void stop() {
        UiAsync.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

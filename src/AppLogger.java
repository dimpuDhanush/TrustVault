import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {
    private static volatile boolean configured;

    private AppLogger() {
    }

    public static Logger get(Class<?> type) {
        configure();
        return Logger.getLogger(type.getName());
    }

    public static synchronized void configure() {
        if (configured) {
            return;
        }

        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);

        boolean hasFileHandler = false;
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof FileHandler) {
                hasFileHandler = true;
                break;
            }
        }

        if (!hasFileHandler) {
            try {
                Files.createDirectories(Path.of("logs"));
                FileHandler fileHandler = new FileHandler("logs/trustvault.%g.log", 1_024 * 1_024, 3, true);
                fileHandler.setFormatter(new SimpleFormatter());
                fileHandler.setEncoding("UTF-8");
                rootLogger.addHandler(fileHandler);
            } catch (Exception exception) {
                System.err.println("Unable to initialize file logging: " + exception.getMessage());
            }
        }

        configured = true;
    }
}

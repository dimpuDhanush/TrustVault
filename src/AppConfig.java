import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class AppConfig {
    private static final Properties FILE_PROPERTIES = loadFileProperties();

    private AppConfig() {
    }

    public static String dbUrl() {
        return resolve("trustvault.db.url", "TV_DB_URL", "db.url", "jdbc:mysql://localhost:3306/trustvault");
    }

    public static String dbUser() {
        return resolve("trustvault.db.user", "TV_DB_USER", "db.user", "root");
    }

    public static String dbPassword() {
        return resolve("trustvault.db.password", "TV_DB_PASSWORD", "db.password", "");
    }

    private static String resolve(String systemKey, String envKey, String fileKey, String defaultValue) {
        String systemValue = System.getProperty(systemKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String fileValue = FILE_PROPERTIES.getProperty(fileKey);
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue.trim();
        }

        return defaultValue;
    }

    private static Properties loadFileProperties() {
        Properties properties = new Properties();
        for (Path configPath : candidateConfigPaths()) {
            if (!Files.exists(configPath)) {
                continue;
            }

            try (InputStream inputStream = Files.newInputStream(configPath)) {
                properties.load(inputStream);
                return properties;
            } catch (Exception exception) {
                AppLogger.get(AppConfig.class).warning("Unable to read " + configPath + ": " + exception.getMessage());
            }
        }
        return properties;
    }

    private static List<Path> candidateConfigPaths() {
        List<Path> paths = new ArrayList<>();
        paths.add(Path.of("trustvault.properties"));

        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path launcherPath = Path.of(appPath).toAbsolutePath();
            Path launcherDir = launcherPath.getParent();
            if (launcherDir != null) {
                paths.add(launcherDir.resolve("trustvault.properties"));
                Path parentDir = launcherDir.getParent();
                if (parentDir != null) {
                    paths.add(parentDir.resolve("trustvault.properties"));
                }
            }
        }

        return paths;
    }
}

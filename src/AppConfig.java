import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class AppConfig {
    private static final String CONFIG_FILE_NAME = "trustvault.properties";
    private static final String CONFIG_DIRECTORY_NAME = "config";
    private static final String CONFIG_FILE_SYSTEM_KEY = "trustvault.config.file";
    private static final String CONFIG_FILE_ENV_KEY = "TV_CONFIG_FILE";
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
        Set<Path> paths = new LinkedHashSet<>();
        addCandidate(paths, System.getProperty(CONFIG_FILE_SYSTEM_KEY));
        addCandidate(paths, System.getenv(CONFIG_FILE_ENV_KEY));
        addCandidate(paths, Path.of(CONFIG_FILE_NAME));
        addCandidate(paths, Path.of(CONFIG_DIRECTORY_NAME, CONFIG_FILE_NAME));

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            addCandidate(paths, Path.of(userHome, ".trustvault", CONFIG_FILE_NAME));
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            addCandidate(paths, Path.of(appData, "TrustVault", CONFIG_FILE_NAME));
        }

        String programData = System.getenv("ProgramData");
        if (programData != null && !programData.isBlank()) {
            addCandidate(paths, Path.of(programData, "TrustVault", CONFIG_FILE_NAME));
        }

        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path launcherPath = Path.of(appPath).toAbsolutePath();
            Path launcherDir = launcherPath.getParent();
            if (launcherDir != null) {
                addCandidate(paths, launcherDir.resolve(CONFIG_FILE_NAME));
                addCandidate(paths, launcherDir.resolve(CONFIG_DIRECTORY_NAME).resolve(CONFIG_FILE_NAME));
                Path parentDir = launcherDir.getParent();
                if (parentDir != null) {
                    addCandidate(paths, parentDir.resolve(CONFIG_FILE_NAME));
                    addCandidate(paths, parentDir.resolve(CONFIG_DIRECTORY_NAME).resolve(CONFIG_FILE_NAME));
                }
            }
        }

        return new ArrayList<>(paths);
    }

    private static void addCandidate(Set<Path> paths, String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return;
        }
        addCandidate(paths, Path.of(rawPath));
    }

    private static void addCandidate(Set<Path> paths, Path path) {
        if (path == null) {
            return;
        }
        paths.add(path.toAbsolutePath().normalize());
    }
}

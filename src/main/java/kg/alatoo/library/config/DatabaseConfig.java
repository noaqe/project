package kg.alatoo.library.config;

import kg.alatoo.library.exception.LibraryException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Environment variables override the untracked db.properties file. */
public final class DatabaseConfig {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseConfig(String url, String user, String password) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) throw new LibraryException("Нужен PostgreSQL JDBC URL.");
        if (user == null || user.isBlank()) throw new LibraryException("Не указан DB_USER.");
        if (password == null || password.isBlank()) throw new LibraryException("Укажите DB_PASSWORD или заполните db.properties по примеру.");
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        Path file = Path.of("db.properties");
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) { properties.load(input); }
            catch (IOException e) { throw new LibraryException("Не удалось прочитать db.properties.", e); }
        }
        Map<String, String> env = System.getenv();
        return new DatabaseConfig(
                env.getOrDefault("DB_URL", properties.getProperty("db.url", "jdbc:postgresql://localhost:5433/library_db")),
                env.getOrDefault("DB_USER", properties.getProperty("db.user", "library_app")),
                env.getOrDefault("DB_PASSWORD", properties.getProperty("db.password")));
    }

    public String getUrl() { return url; }
    public String getUser() { return user; }
    public String getPassword() { return password; }
}

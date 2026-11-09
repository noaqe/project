package kg.alatoo.library.db;

import kg.alatoo.library.config.DatabaseConfig;
import kg.alatoo.library.exception.LibraryException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Opens short-lived connections and provides a single transaction boundary for DAO operations. */
public final class Database {
    private final DatabaseConfig config;
    public Database(DatabaseConfig config) { this.config = config; }

    public Connection open() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", config.getUser());
        properties.setProperty("password", config.getPassword());
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "30");
        Connection connection = DriverManager.getConnection(config.getUrl(), properties);
        try { connection.setSchema("library"); return connection; }
        catch (SQLException e) { connection.close(); throw e; }
    }

    @FunctionalInterface
    public interface SqlWork<T> { T run(Connection connection) throws SQLException; }

    public <T> T query(SqlWork<T> work) {
        try (Connection connection = open()) { return work.run(connection); }
        catch (SQLException e) { throw translate(e); }
    }

    public <T> T transaction(SqlWork<T> work) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            }
        } catch (SQLException e) { throw translate(e); }
    }

    private LibraryException translate(SQLException error) {
        String state = error.getSQLState();
        String message;
        if ("23505".equals(state)) message = "Такая запись уже существует (ISBN, email, имя или активная выдача).";
        else if ("23503".equals(state)) message = "Запись связана с другими данными либо указанный ID не существует. История выдач сохраняется.";
        else if ("23514".equals(state) || "23502".equals(state)) message = "Данные нарушают ограничения базы.";
        else if ("42P01".equals(state) || "3F000".equals(state)) message = "Схема не создана. Выполните запуск с --init.";
        else if (state != null && (state.startsWith("08") || state.startsWith("28"))) message = "Не удалось подключиться к PostgreSQL. Проверьте адрес, пользователя, пароль и запуск БД.";
        else message = "Ошибка PostgreSQL (SQLState " + state + ").";
        return new LibraryException(message, error);
    }
}

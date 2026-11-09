package kg.alatoo.library.db;

import kg.alatoo.library.exception.LibraryException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;

public final class SchemaInitializer {
    private final Database database;
    public SchemaInitializer(Database database) { this.database = database; }

    public void initialize() { executeResource("/db/schema.sql"); }
    public void seed() { executeResource("/db/seed.sql"); }

    private void executeResource(String name) {
        String sql;
        try (InputStream stream = SchemaInitializer.class.getResourceAsStream(name)) {
            if (stream == null) throw new LibraryException("Не найден SQL-ресурс " + name);
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new LibraryException("Не удалось прочитать SQL-ресурс.", e); }
        database.transaction(connection -> {
            // Fixed bundled DDL/DML contains no user input. Runtime queries use PreparedStatement.
            try (Statement statement = connection.createStatement()) { statement.execute(sql); }
            return null;
        });
    }
}

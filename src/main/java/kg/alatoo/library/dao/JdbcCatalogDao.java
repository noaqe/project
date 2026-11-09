package kg.alatoo.library.dao;

import kg.alatoo.library.db.Database;
import kg.alatoo.library.exception.LibraryException;
import kg.alatoo.library.model.Author;
import kg.alatoo.library.model.Category;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Small reference catalogs; the required full CRUD is implemented for books and readers. */
public final class JdbcCatalogDao {
    private final Database database;
    public JdbcCatalogDao(Database database) { this.database = database; }

    public List<Author> findAuthors() {
        return database.query(c -> {
            List<Author> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, name FROM authors ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new Author(rs.getLong("id"), rs.getString("name")));
            }
            return result;
        });
    }

    public List<Category> findCategories() {
        return database.query(c -> {
            List<Category> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, name FROM categories ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new Category(rs.getLong("id"), rs.getString("name")));
            }
            return result;
        });
    }

    public Author createAuthor(String name) {
        Author author = new Author(0, name);
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO authors(name) VALUES (?) RETURNING id")) {
                ps.setString(1, author.name());
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return new Author(rs.getLong(1), author.name()); }
            }
        });
    }

    public Category createCategory(String name) {
        Category category = new Category(0, name);
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO categories(name) VALUES (?) RETURNING id")) {
                ps.setString(1, category.name());
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return new Category(rs.getLong(1), category.name()); }
            }
        });
    }

    public Category requireCategory(long id) {
        return findCategories().stream().filter(c -> c.id() == id).findFirst()
                .orElseThrow(() -> new LibraryException("Категория #" + id + " не найдена."));
    }

    public List<Author> requireAuthors(List<Long> ids) {
        List<Author> all = findAuthors();
        List<Author> result = new ArrayList<>();
        for (long id : ids) {
            result.add(all.stream().filter(a -> a.id() == id).findFirst()
                    .orElseThrow(() -> new LibraryException("Автор #" + id + " не найден.")));
        }
        return result;
    }
}

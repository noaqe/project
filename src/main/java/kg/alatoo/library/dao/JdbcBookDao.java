package kg.alatoo.library.dao;

import kg.alatoo.library.db.Database;
import kg.alatoo.library.exception.LibraryException;
import kg.alatoo.library.model.Author;
import kg.alatoo.library.model.Book;
import kg.alatoo.library.model.Category;
import kg.alatoo.library.util.Validation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JdbcBookDao implements BookDao {
    private static final String SELECT = """
            SELECT b.id, b.isbn, b.title, b.publication_year, b.total_copies,
                   c.id AS category_id, c.name AS category_name,
                   a.id AS author_id, a.name AS author_name
            FROM books b JOIN categories c ON c.id = b.category_id
            JOIN book_authors ba ON ba.book_id = b.id JOIN authors a ON a.id = ba.author_id
            """;
    private final Database database;
    public JdbcBookDao(Database database) { this.database = database; }

    @Override public Book create(Book book) {
        return database.transaction(c -> {
            long id;
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO books(isbn, title, publication_year, category_id, total_copies)
                    VALUES (?, ?, ?, ?, ?) RETURNING id
                    """)) {
                bind(ps, book);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); id = rs.getLong(1); }
            }
            insertAuthors(c, id, book.getAuthors());
            return find(c, id).orElseThrow();
        });
    }

    @Override public Optional<Book> findById(long id) {
        Validation.positiveId(id);
        return database.query(c -> find(c, id));
    }

    private Optional<Book> find(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE b.id = ? ORDER BY b.id, a.id")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return mapBooks(rs).stream().findFirst(); }
        }
    }

    @Override public List<Book> findAll() {
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY b.id, a.id");
                 ResultSet rs = ps.executeQuery()) { return mapBooks(rs); }
        });
    }

    @Override public List<Book> search(String query) {
        String pattern = literalPattern(Validation.text(query, "Поиск", 200));
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT + """
                     WHERE b.title ILIKE ? ESCAPE '!' OR b.isbn ILIKE ? ESCAPE '!'
                     OR EXISTS (SELECT 1 FROM book_authors x JOIN authors y ON y.id = x.author_id
                                WHERE x.book_id = b.id AND y.name ILIKE ? ESCAPE '!')
                     ORDER BY b.id, a.id
                    """)) {
                for (int i = 1; i <= 3; i++) ps.setString(i, pattern);
                try (ResultSet rs = ps.executeQuery()) { return mapBooks(rs); }
            }
        });
    }

    static String literalPattern(String value) {
        return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    @Override public void update(Book book) {
        Validation.positiveId(book.getId());
        database.transaction(c -> {
            // Issue operations lock the same book row before counting active loans.
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM books WHERE id = ? FOR UPDATE")) {
                ps.setLong(1, book.getId());
                try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new LibraryException("Книга не найдена."); }
            }
            if (book.getTotalCopies() < activeLoans(c, book.getId())) {
                throw new LibraryException("Нельзя оставить меньше экземпляров, чем сейчас выдано.");
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE books SET isbn = ?, title = ?, publication_year = ?, category_id = ?, total_copies = ? WHERE id = ?
                    """)) {
                bind(ps, book); ps.setLong(6, book.getId()); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM book_authors WHERE book_id = ?")) {
                ps.setLong(1, book.getId()); ps.executeUpdate();
            }
            insertAuthors(c, book.getId(), book.getAuthors());
            return null;
        });
    }

    @Override public void delete(long id) {
        Validation.positiveId(id);
        database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE id = ?")) {
                ps.setLong(1, id);
                if (ps.executeUpdate() == 0) throw new LibraryException("Книга не найдена.");
            }
            return null;
        });
    }

    @Override public int availableCopies(long id) {
        Validation.positiveId(id);
        return database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT b.total_copies - (SELECT count(*) FROM loans l WHERE l.book_id = b.id AND l.returned_on IS NULL) AS available
                    FROM books b WHERE b.id = ?
                    """)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new LibraryException("Книга не найдена.");
                    return rs.getInt("available");
                }
            }
        });
    }

    static int activeLoans(Connection c, long bookId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM loans WHERE book_id = ? AND returned_on IS NULL")) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private void bind(PreparedStatement ps, Book book) throws SQLException {
        ps.setString(1, book.getIsbn()); ps.setString(2, book.getTitle());
        ps.setInt(3, book.getPublicationYear()); ps.setLong(4, book.getCategory().id()); ps.setInt(5, book.getTotalCopies());
    }

    private void insertAuthors(Connection c, long bookId, List<Author> authors) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO book_authors(book_id, author_id) VALUES (?, ?)")) {
            for (Author author : authors) { ps.setLong(1, bookId); ps.setLong(2, author.id()); ps.addBatch(); }
            ps.executeBatch();
        }
    }

    private record BookRow(String isbn, String title, int year, Category category, int copies) { }

    private List<Book> mapBooks(ResultSet rs) throws SQLException {
        Map<Long, BookRow> rows = new LinkedHashMap<>();
        Map<Long, List<Author>> authors = new LinkedHashMap<>();
        while (rs.next()) {
            long id = rs.getLong("id");
            rows.putIfAbsent(id, new BookRow(rs.getString("isbn"), rs.getString("title"), rs.getInt("publication_year"),
                    new Category(rs.getLong("category_id"), rs.getString("category_name")), rs.getInt("total_copies")));
            authors.computeIfAbsent(id, key -> new ArrayList<>()).add(new Author(rs.getLong("author_id"), rs.getString("author_name")));
        }
        List<Book> result = new ArrayList<>();
        for (Map.Entry<Long, BookRow> entry : rows.entrySet()) {
            BookRow b = entry.getValue();
            result.add(new Book(entry.getKey(), b.isbn(), b.title(), b.year(), b.category(), b.copies(), authors.get(entry.getKey())));
        }
        return result;
    }
}

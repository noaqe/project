package kg.alatoo.library.dao;

import kg.alatoo.library.db.Database;
import kg.alatoo.library.exception.LibraryException;
import kg.alatoo.library.model.Loan;
import kg.alatoo.library.model.MembershipType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class JdbcLoanDao implements LoanDao {
    private final Database database;
    public JdbcLoanDao(Database database) { this.database = database; }

    /** Locks the reader first, then the book, so parallel requests cannot oversubscribe either. */
    @Override public long issue(long bookId, long readerId, MembershipType expectedType, int limit, int days, LocalDate today) {
        return database.transaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT membership_type FROM readers WHERE id = ? FOR UPDATE")) {
                ps.setLong(1, readerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new LibraryException("Читатель не найден.");
                    if (!rs.getString(1).equals(expectedType.name())) throw new LibraryException("Тип читателя изменился. Повторите выдачу.");
                }
            }
            int copies;
            try (PreparedStatement ps = c.prepareStatement("SELECT total_copies FROM books WHERE id = ? FOR UPDATE")) {
                ps.setLong(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new LibraryException("Книга не найдена.");
                    copies = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT count(*) AS active,
                           count(*) FILTER (WHERE due_on < ?) AS overdue,
                           count(*) FILTER (WHERE book_id = ?) AS same_book
                    FROM loans WHERE reader_id = ? AND returned_on IS NULL
                    """)) {
                ps.setObject(1, today); ps.setLong(2, bookId); ps.setLong(3, readerId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt("same_book") > 0) throw new LibraryException("Эта книга уже выдана этому читателю.");
                    if (rs.getInt("overdue") > 0) throw new LibraryException("Сначала верните просроченные книги.");
                    if (rs.getInt("active") >= limit) throw new LibraryException("Достигнут лимит читателя: " + limit + " книг.");
                }
            }
            if (JdbcBookDao.activeLoans(c, bookId) >= copies) throw new LibraryException("Свободных экземпляров нет.");
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO loans(book_id, reader_id, borrowed_on, due_on) VALUES (?, ?, ?, ?) RETURNING id
                    """)) {
                ps.setLong(1, bookId); ps.setLong(2, readerId);
                ps.setObject(3, today); ps.setObject(4, today.plusDays(days));
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
            }
        });
    }

    @Override public void returnBook(long loanId, LocalDate today) {
        database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE loans SET returned_on = ? WHERE id = ? AND returned_on IS NULL")) {
                ps.setObject(1, today); ps.setLong(2, loanId);
                if (ps.executeUpdate() == 0) throw new LibraryException("Выдача не найдена либо книга уже возвращена.");
            }
            return null;
        });
    }

    @Override public List<Loan> findAll(boolean activeOnly) {
        return database.query(c -> {
            String sql = """
                    SELECT l.*, b.title, r.full_name
                    FROM loans l JOIN books b ON b.id = l.book_id JOIN readers r ON r.id = l.reader_id
                    """ + (activeOnly ? " WHERE l.returned_on IS NULL" : "") + " ORDER BY l.id";
            List<Loan> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapLoan(rs));
            }
            return result;
        });
    }

    static Loan mapLoan(ResultSet rs) throws SQLException {
        return new Loan(rs.getLong("id"), rs.getLong("book_id"), rs.getString("title"),
                rs.getLong("reader_id"), rs.getString("full_name"), rs.getObject("borrowed_on", LocalDate.class),
                rs.getObject("due_on", LocalDate.class), rs.getObject("returned_on", LocalDate.class));
    }
}

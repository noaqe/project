package kg.alatoo.library.dao;

import kg.alatoo.library.db.Database;
import kg.alatoo.library.model.Loan;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class JdbcReportDao {
    public record PopularBook(long id, String title, long loanCount) {
        @Override public String toString() { return id + " | " + title + " | выдач: " + loanCount; }
    }
    public record CategoryStats(String category, long titles, long copies, long activeLoans, long totalLoans) {
        public long available() { return copies - activeLoans; }
        @Override public String toString() {
            return category + " | названий: " + titles + " | экземпляров: " + copies
                    + " | на руках: " + activeLoans + " | свободно: " + available() + " | всех выдач: " + totalLoans;
        }
    }

    private final Database database;
    public JdbcReportDao(Database database) { this.database = database; }

    public List<PopularBook> popularBooks() {
        return database.query(c -> {
            List<PopularBook> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT b.id, b.title, count(l.id) AS loan_count
                    FROM books b LEFT JOIN loans l ON l.book_id = b.id
                    GROUP BY b.id, b.title ORDER BY loan_count DESC, b.title, b.id
                    """); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new PopularBook(rs.getLong("id"), rs.getString("title"), rs.getLong("loan_count")));
            }
            return result;
        });
    }

    public List<CategoryStats> categoryStats() {
        return database.query(c -> {
            List<CategoryStats> result = new ArrayList<>();
            // Aggregate loans before joining categories: several loans or authors must not multiply book copies.
            try (PreparedStatement ps = c.prepareStatement("""
                    WITH loan_counts AS (
                        SELECT book_id, count(*) AS total_loans,
                               count(*) FILTER (WHERE returned_on IS NULL) AS active_loans
                        FROM loans GROUP BY book_id
                    )
                    SELECT c.name, count(b.id) AS titles, coalesce(sum(b.total_copies), 0) AS copies,
                           coalesce(sum(l.active_loans), 0) AS active_loans,
                           coalesce(sum(l.total_loans), 0) AS total_loans
                    FROM categories c LEFT JOIN books b ON b.category_id = c.id
                    LEFT JOIN loan_counts l ON l.book_id = b.id
                    GROUP BY c.id, c.name ORDER BY c.name
                    """); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new CategoryStats(rs.getString("name"), rs.getLong("titles"), rs.getLong("copies"),
                        rs.getLong("active_loans"), rs.getLong("total_loans")));
            }
            return result;
        });
    }

    public List<Loan> overdue(LocalDate today) {
        return database.query(c -> {
            List<Loan> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT l.*, b.title, r.full_name FROM loans l
                    JOIN books b ON b.id = l.book_id JOIN readers r ON r.id = l.reader_id
                    WHERE l.returned_on IS NULL AND l.due_on < ? ORDER BY l.due_on, l.id
                    """)) {
                ps.setObject(1, today);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(JdbcLoanDao.mapLoan(rs)); }
            }
            return result;
        });
    }
}

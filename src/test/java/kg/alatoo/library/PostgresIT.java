package kg.alatoo.library;

import kg.alatoo.library.config.DatabaseConfig;
import kg.alatoo.library.dao.*;
import kg.alatoo.library.db.Database;
import kg.alatoo.library.db.SchemaInitializer;
import kg.alatoo.library.exception.LibraryException;
import kg.alatoo.library.model.*;
import kg.alatoo.library.service.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** These tests reset ONLY the library schema in an explicitly opted-in local *_test database. */
class PostgresIT {
    private static Database database;
    private JdbcBookDao books;
    private JdbcReaderDao readers;
    private JdbcCatalogDao catalog;
    private JdbcLoanDao loans;
    private JdbcReportDao reports;
    private Category category;
    private Author author;
    private long isbnCounter;
    private final LocalDate today = LocalDate.of(2026, 11, 9);

    @BeforeAll static void connect() {
        String url = System.getenv("TEST_DB_URL");
        assertNotNull(url, "Set TEST_DB_URL to an isolated local *_test database.");
        assertTrue(url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/[a-zA-Z0-9_]+_test"),
                "Integration tests require a loopback URL and a database name ending in _test (no JDBC URL parameters).");
        assertEquals("true", System.getenv("TEST_DB_RESET"), "Set TEST_DB_RESET=true to allow resetting the test schema.");
        database = new Database(new DatabaseConfig(url, System.getenv("TEST_DB_USER"), System.getenv("TEST_DB_PASSWORD")));
    }

    @BeforeEach void reset() {
        database.query(c -> { try (Statement s = c.createStatement()) { s.execute("DROP SCHEMA IF EXISTS library CASCADE"); } return null; });
        new SchemaInitializer(database).initialize();
        books = new JdbcBookDao(database); readers = new JdbcReaderDao(database); catalog = new JdbcCatalogDao(database);
        loans = new JdbcLoanDao(database); reports = new JdbcReportDao(database);
        category = catalog.createCategory("Programming"); author = catalog.createAuthor("First Author");
        isbnCounter = 1;
    }

    private String isbn() {
        String first = String.format("978%09d", isbnCounter++);
        int sum = 0; for (int i = 0; i < 12; i++) sum += (first.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return first + (10 - sum % 10) % 10;
    }
    private Book book(String title, int copies) {
        return books.create(new Book(0, isbn(), title, 2020, category, copies, List.of(author)));
    }
    private Reader reader(String name, MembershipType type) {
        return readers.create(new Reader(0, name, name.toLowerCase() + "@example.com", type));
    }
    private LoanService service() {
        return new LoanService(readers, loans, Map.of(MembershipType.STUDENT, new StudentLendingPolicy(),
                MembershipType.GUEST, new GuestLendingPolicy()), Clock.fixed(Instant.parse("2026-11-09T06:00:00Z"), ZoneOffset.UTC));
    }

    @Test void fullBookCrudAndManyToManyMapping() {
        Author second = catalog.createAuthor("Second Author"); Book book = book("Before", 2);
        books.update(new Book(book.getId(), book.getIsbn(), "After", 2021, category, 3, List.of(author, second)));
        Book loaded = books.findById(book.getId()).orElseThrow();
        assertEquals("After", loaded.getTitle()); assertEquals(2, loaded.getAuthors().size());
        assertEquals(3, books.availableCopies(book.getId()));
        assertEquals(1, books.findAll().size());
        books.delete(book.getId()); assertTrue(books.findById(book.getId()).isEmpty());
        assertEquals(2, catalog.findAuthors().size());
    }

    @Test void fullReaderCrudAndCaseInsensitiveSearch() {
        Reader reader = reader("Aida", MembershipType.STUDENT);
        readers.update(new Reader(reader.getId(), "Aida Asanova", "NEW@EXAMPLE.COM", MembershipType.GUEST));
        assertEquals("new@example.com", readers.findById(reader.getId()).orElseThrow().getEmail());
        assertEquals(1, readers.search("ASANOVA").size()); assertEquals(1, readers.findAll().size());
        readers.delete(reader.getId()); assertTrue(readers.findById(reader.getId()).isEmpty());
    }

    @Test void joinedSearchKeepsAllCoauthorsAndTreatsWildcardsLiterally() {
        Author second = catalog.createAuthor("Unique Coauthor");
        Book b = books.create(new Book(0, isbn(), "100% Java_guide", 2020, category, 1, List.of(author, second)));
        book("Other", 1);
        assertEquals(2, books.search("unique coauthor").get(0).getAuthors().size());
        assertEquals(b, books.search("%").get(0)); assertEquals(1, books.search("_").size());
        assertTrue(books.search("' OR 1=1 --").isEmpty());
        assertEquals(1, books.search(b.getIsbn()).size());
    }

    @Test void duplicateIsbnAndNormalizedEmailAreRejected() {
        Book b = book("X", 1); reader("Aida", MembershipType.STUDENT);
        assertThrows(LibraryException.class, () -> books.create(new Book(0, b.getIsbn(), "Y", 2020, category, 1, List.of(author))));
        assertThrows(LibraryException.class, () -> readers.create(new Reader(0, "Other", "AIDA@EXAMPLE.COM", MembershipType.GUEST)));
    }

    @Test void failedAuthorInsertRollsBackWholeBook() {
        assertThrows(LibraryException.class, () -> books.create(new Book(0, isbn(), "Rollback", 2020, category, 1,
                List.of(author, new Author(9999, "Missing")))));
        assertTrue(books.findAll().isEmpty());
    }

    @Test void failedBookUpdateRestoresOldFieldsAndAuthors() {
        Book b = book("Original", 1);
        assertThrows(LibraryException.class, () -> books.update(new Book(b.getId(), b.getIsbn(), "Changed", 2020, category, 2,
                List.of(new Author(9999, "Missing")))));
        Book loaded = books.findById(b.getId()).orElseThrow();
        assertEquals("Original", loaded.getTitle()); assertEquals(1, loaded.getTotalCopies()); assertEquals(List.of(author), loaded.getAuthors());
    }

    @Test void issueAndReturnUpdateAvailabilityAndDueDate() {
        Book b = book("X", 1); Reader r = reader("Aida", MembershipType.STUDENT);
        long loanId = service().issue(b.getId(), r.getId());
        assertEquals(0, books.availableCopies(b.getId()));
        assertEquals(today.plusDays(30), loans.findAll(true).get(0).dueOn());
        assertThrows(LibraryException.class, () -> service().issue(b.getId(), r.getId()));
        service().returnBook(loanId); assertEquals(1, books.availableCopies(b.getId()));
        assertTrue(loans.findAll(true).isEmpty()); assertEquals(today, loans.findAll(false).get(0).returnedOn());
        assertThrows(LibraryException.class, () -> service().returnBook(loanId));
    }

    @Test void guestLimitIsTwoAndLoanPeriodIsFourteenDays() {
        Reader r = reader("Bek", MembershipType.GUEST);
        service().issue(book("First", 1).getId(), r.getId()); service().issue(book("Second", 1).getId(), r.getId());
        assertEquals(today.plusDays(14), loans.findAll(true).get(0).dueOn());
        Book third = book("Third", 1);
        assertThrows(LibraryException.class, () -> service().issue(third.getId(), r.getId()));
        assertEquals(1, books.availableCopies(third.getId()));
    }

    @Test void cannotReduceCopiesBelowActiveLoansOrDeleteHistory() {
        Book b = book("X", 2); Reader r1 = reader("Aida", MembershipType.STUDENT); Reader r2 = reader("Bek", MembershipType.GUEST);
        long first = service().issue(b.getId(), r1.getId()); long second = service().issue(b.getId(), r2.getId());
        assertThrows(LibraryException.class, () -> books.update(new Book(b.getId(), b.getIsbn(), "X", 2020, category, 1, List.of(author))));
        assertThrows(LibraryException.class, () -> books.delete(b.getId()));
        service().returnBook(first); service().returnBook(second);
        assertThrows(LibraryException.class, () -> readers.delete(r1.getId()));
        assertThrows(LibraryException.class, () -> books.delete(b.getId()));
    }

    @Test void overdueBlocksNewLoansAndAppearsInReport() {
        Reader r = reader("Bek", MembershipType.GUEST);
        LoanService oldService = new LoanService(readers, loans, Map.of(MembershipType.GUEST, new GuestLendingPolicy()),
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        oldService.issue(book("Old", 1).getId(), r.getId());
        assertEquals(1, reports.overdue(today).size());
        Book next = book("Next", 1);
        assertThrows(LibraryException.class, () -> service().issue(next.getId(), r.getId()));
    }

    @Test void aggregateReportsDoNotMultiplyCopiesByLoansOrAuthors() {
        Author second = catalog.createAuthor("Second");
        Book b = books.create(new Book(0, isbn(), "X", 2020, category, 3, List.of(author, second)));
        Reader r = reader("Aida", MembershipType.STUDENT);
        long first = service().issue(b.getId(), r.getId()); service().returnBook(first); service().issue(b.getId(), r.getId());
        catalog.createCategory("Empty");
        JdbcReportDao.CategoryStats stats = reports.categoryStats().stream().filter(s -> s.category().equals("Programming")).findFirst().orElseThrow();
        assertEquals(1, stats.titles()); assertEquals(3, stats.copies()); assertEquals(1, stats.activeLoans());
        assertEquals(2, stats.available()); assertEquals(2, stats.totalLoans());
        assertEquals(2, reports.popularBooks().get(0).loanCount());
        assertEquals(0, reports.categoryStats().stream().filter(s -> s.category().equals("Empty")).findFirst().orElseThrow().copies());
    }

    @Test void schemaAndDemoInitializationCanBeRepeated() {
        SchemaInitializer initializer = new SchemaInitializer(database);
        initializer.initialize(); initializer.seed(); initializer.seed();
        assertEquals(4, books.findAll().size()); assertEquals(2, readers.findAll().size()); assertEquals(3, loans.findAll(false).size());
    }

    @Test void concurrentRequestsCannotIssueTheLastCopyTwice() throws Exception {
        Book b = book("Only copy", 1);
        Reader a = reader("Aida", MembershipType.STUDENT); Reader z = reader("Bek", MembershipType.STUDENT);
        assertEquals(1, race(() -> service().issue(b.getId(), a.getId()), () -> service().issue(b.getId(), z.getId())));
        assertEquals(0, books.availableCopies(b.getId())); assertEquals(1, loans.findAll(true).size());
    }

    @Test void concurrentRequestsCannotExceedReaderLimit() throws Exception {
        Reader r = reader("Bek", MembershipType.GUEST);
        service().issue(book("Existing", 1).getId(), r.getId());
        Book b1 = book("One", 1); Book b2 = book("Two", 1);
        assertEquals(1, race(() -> service().issue(b1.getId(), r.getId()), () -> service().issue(b2.getId(), r.getId())));
        assertEquals(2, loans.findAll(true).size());
    }

    @Test void missingIdsHaveUsefulErrorsAndDoNotCreateLoans() {
        Book b = book("X", 1); Reader r = reader("Aida", MembershipType.STUDENT);
        assertThrows(LibraryException.class, () -> service().issue(99999, r.getId()));
        assertThrows(LibraryException.class, () -> service().issue(b.getId(), 99999));
        assertThrows(LibraryException.class, () -> service().returnBook(99999)); assertTrue(loans.findAll(false).isEmpty());
    }

    @Test void databaseRejectsInvalidForeignKeysAndDates() {
        Book b = book("X", 1); Reader r = reader("Aida", MembershipType.GUEST);
        assertThrows(LibraryException.class, () -> database.query(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO loans(book_id, reader_id, borrowed_on, due_on) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, b.getId()); ps.setLong(2, r.getId()); ps.setObject(3, today); ps.setObject(4, today.minusDays(1)); ps.executeUpdate();
            }
            return null;
        }));
    }

    private int race(Callable<Long> first, Callable<Long> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = executor.submit(() -> { start.await(); return attempt(first); });
            Future<Boolean> b = executor.submit(() -> { start.await(); return attempt(second); });
            start.countDown(); return (a.get(15, TimeUnit.SECONDS) ? 1 : 0) + (b.get(15, TimeUnit.SECONDS) ? 1 : 0);
        } finally { executor.shutdownNow(); }
    }
    private boolean attempt(Callable<Long> work) throws Exception {
        try { work.call(); return true; } catch (LibraryException expected) { return false; }
    }
}

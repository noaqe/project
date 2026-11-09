package kg.alatoo.library;

import kg.alatoo.library.model.*;
import kg.alatoo.library.util.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
    private Book book(String isbn, String title, int year, int copies, List<Author> authors) {
        return new Book(0, isbn, title, year, new Category(1, "Programming"), copies, authors);
    }

    @Test void isbnAcceptsSpacesAndHyphens() {
        assertEquals("9780132350884", Validation.isbn("978-0-13-235088-4"));
    }

    @ParameterizedTest @ValueSource(strings = {"9780132350885", "123", "abcdefghijklm", ""})
    void rejectsInvalidIsbn(String value) { assertThrows(IllegalArgumentException.class, () -> Validation.isbn(value)); }

    @ParameterizedTest @ValueSource(strings = {"", "   ", "title\ntext"})
    void rejectsBlankOrControlText(String value) { assertThrows(IllegalArgumentException.class, () -> new Author(0, value)); }

    @ParameterizedTest @ValueSource(strings = {"missing-at", "a@b", "a b@example.com", "a@@b.com"})
    void rejectsInvalidEmail(String value) { assertThrows(IllegalArgumentException.class, () -> Validation.email(value)); }

    @Test void normalizesReaderEmailAndUsesPolymorphism() {
        Person person = new Reader(0, " Aida ", " AIDA@Example.com ", MembershipType.STUDENT);
        assertEquals("Aida", person.getFullName());
        assertEquals("aida@example.com", person.getEmail());
        assertEquals("Читатель (STUDENT)", person.getRole());
        assertTrue(person.toString().contains("STUDENT"));
    }

    @Test void bookDefensivelyCopiesAuthors() {
        List<Author> authors = new ArrayList<>(List.of(new Author(1, "Martin")));
        Book book = book("9780132350884", "Clean Code", 2008, 1, authors);
        authors.clear();
        assertEquals(1, book.getAuthors().size());
        assertThrows(UnsupportedOperationException.class, () -> book.getAuthors().clear());
    }

    @Test void equalityAndHashCodeUseSameIsbn() {
        Book first = book("9780132350884", "First", 2008, 1, List.of(new Author(1, "Martin")));
        Book second = book("9780132350884", "Second", 2009, 2, List.of(new Author(1, "Martin")));
        assertEquals(first, second); assertEquals(first.hashCode(), second.hashCode());
        assertEquals(1, new HashSet<>(List.of(first, second)).size());
    }

    @Test void rejectsDuplicateOrMissingAuthors() {
        Author author = new Author(1, "Martin");
        assertThrows(IllegalArgumentException.class, () -> book("9780132350884", "X", 2000, 1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> book("9780132350884", "X", 2000, 1, List.of(author, author)));
    }

    @ParameterizedTest @ValueSource(ints = {0, -1, 10001})
    void rejectsInvalidCopies(int count) {
        assertThrows(IllegalArgumentException.class, () -> book("9780132350884", "X", 2000, count, List.of(new Author(1, "Martin"))));
    }

    @ParameterizedTest @ValueSource(ints = {1449, 2101})
    void rejectsInvalidYear(int year) {
        assertThrows(IllegalArgumentException.class, () -> book("9780132350884", "X", year, 1, List.of(new Author(1, "Martin"))));
    }

    @Test void dueTodayIsNotOverdueAndReturnedLoanIsNotOverdue() {
        LocalDate today = LocalDate.of(2026, 11, 9);
        Loan dueToday = new Loan(1, 1, "X", 1, "Y", today.minusDays(14), today, null);
        assertFalse(dueToday.isOverdue(today)); assertTrue(dueToday.isOverdue(today.plusDays(1)));
        Loan returned = new Loan(2, 1, "X", 1, "Y", today.minusDays(30), today.minusDays(1), today);
        assertFalse(returned.isOverdue(today));
    }
}

package kg.alatoo.library.model;

import java.time.LocalDate;

public record Loan(long id, long bookId, String bookTitle, long readerId, String readerName,
                   LocalDate borrowedOn, LocalDate dueOn, LocalDate returnedOn) {
    public boolean isOverdue(LocalDate today) { return returnedOn == null && dueOn.isBefore(today); }

    @Override public String toString() {
        return id + " | книга #" + bookId + " " + bookTitle + " | читатель #" + readerId + " " + readerName
                + " | выдана: " + borrowedOn + " | срок: " + dueOn
                + " | " + (returnedOn == null ? "на руках" : "возврат: " + returnedOn);
    }
}

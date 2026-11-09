package kg.alatoo.library.model;

import kg.alatoo.library.util.Validation;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** An edition with several interchangeable copies; availability is calculated from active loans. */
public final class Book {
    private final long id;
    private final String isbn;
    private final String title;
    private final int publicationYear;
    private final Category category;
    private final int totalCopies;
    private final List<Author> authors;

    public Book(long id, String isbn, String title, int publicationYear,
                Category category, int totalCopies, List<Author> authors) {
        if (id < 0) throw new IllegalArgumentException("ID не может быть отрицательным.");
        this.id = id;
        this.isbn = Validation.isbn(isbn);
        this.title = Validation.text(title, "Название", 200);
        if (publicationYear < 1450 || publicationYear > 2100) throw new IllegalArgumentException("Год: от 1450 до 2100.");
        this.publicationYear = publicationYear;
        this.category = Objects.requireNonNull(category, "Категория обязательна.");
        Validation.positiveId(category.id());
        if (totalCopies < 1 || totalCopies > 10000) throw new IllegalArgumentException("Экземпляры: от 1 до 10000.");
        this.totalCopies = totalCopies;
        if (authors == null || authors.isEmpty()) throw new IllegalArgumentException("Нужен хотя бы один автор.");
        this.authors = List.copyOf(authors);
        if (this.authors.stream().map(Author::id).distinct().count() != this.authors.size()) {
            throw new IllegalArgumentException("Авторы не должны повторяться.");
        }
        this.authors.forEach(a -> Validation.positiveId(a.id()));
    }

    public long getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public int getPublicationYear() { return publicationYear; }
    public Category getCategory() { return category; }
    public int getTotalCopies() { return totalCopies; }
    public List<Author> getAuthors() { return authors; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof Book book && isbn.equals(book.isbn);
    }

    @Override public int hashCode() { return isbn.hashCode(); }

    @Override public String toString() {
        String names = authors.stream().map(Author::name).collect(Collectors.joining(", "));
        return id + " | " + title + " | " + names + " | " + publicationYear
                + " | " + category.name() + " | ISBN " + isbn + " | всего: " + totalCopies;
    }
}

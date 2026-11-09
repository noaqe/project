package kg.alatoo.library.ui;

import kg.alatoo.library.dao.*;
import kg.alatoo.library.exception.LibraryException;
import kg.alatoo.library.model.*;
import kg.alatoo.library.service.LoanService;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;

/** User interaction only; this class has no SQL or JDBC code. */
public final class ConsoleMenu {
    private final ConsoleInput input;
    private final PrintStream out;
    private final BookDao books;
    private final ReaderDao readers;
    private final JdbcCatalogDao catalog;
    private final LoanDao loans;
    private final LoanService lending;
    private final JdbcReportDao reports;

    public ConsoleMenu(ConsoleInput input, PrintStream out, BookDao books, ReaderDao readers,
                       JdbcCatalogDao catalog, LoanDao loans, LoanService lending, JdbcReportDao reports) {
        this.input = input; this.out = out; this.books = books; this.readers = readers;
        this.catalog = catalog; this.loans = loans; this.lending = lending; this.reports = reports;
    }

    public void run() {
        out.println("\nБИБЛИОТЕКА | Java + PostgreSQL");
        try {
            while (true) {
                out.println("\n1. Книги  2. Читатели  3. Выдачи  4. Справочники  5. Отчёты  0. Выход");
                int choice = input.number("> ", 0, 5);
                if (choice == 0) { out.println("До свидания!"); return; }
                safely(() -> {
                    switch (choice) {
                        case 1 -> bookMenu(); case 2 -> readerMenu(); case 3 -> loanMenu();
                        case 4 -> catalogMenu(); case 5 -> reportMenu();
                        default -> throw new IllegalStateException();
                    }
                });
            }
        } catch (ConsoleInput.EndOfInput e) { out.println("\nВвод завершён."); }
    }

    private void bookMenu() {
        while (true) {
            out.println("\nКНИГИ: 1. Все  2. По ID  3. Добавить  4. Изменить  5. Удалить  6. Поиск  7. Наличие  0. Назад");
            int choice = input.number("> ", 0, 7);
            if (choice == 0) return;
            safely(() -> {
                switch (choice) {
                    case 1 -> print(books.findAll());
                    case 2 -> out.println(books.findById(input.id("ID книги: ")).orElseThrow(() -> new LibraryException("Книга не найдена.")));
                    case 3 -> out.println("Добавлено: " + books.create(readBook(0)));
                    case 4 -> {
                        long id = input.id("ID книги: ");
                        out.println(books.findById(id).orElseThrow(() -> new LibraryException("Книга не найдена.")));
                        out.println("Введите все новые значения:"); books.update(readBook(id)); out.println("Книга изменена.");
                    }
                    case 5 -> {
                        long id = input.id("ID книги: ");
                        if (input.confirm("Удалить книгу #" + id + "?")) { books.delete(id); out.println("Книга удалена."); }
                    }
                    case 6 -> print(books.search(input.text("Название, автор или ISBN: ", 200)));
                    case 7 -> out.println("Свободных экземпляров: " + books.availableCopies(input.id("ID книги: ")));
                    default -> throw new IllegalStateException();
                }
            });
        }
    }

    private Book readBook(long id) {
        String isbn = input.text("ISBN-13: ", 30);
        String title = input.text("Название: ", 200);
        int year = input.number("Год издания (1450-2100): ", 1450, 2100);
        print(catalog.findCategories());
        Category category = catalog.requireCategory(input.id("ID категории: "));
        print(catalog.findAuthors());
        List<Author> authors = catalog.requireAuthors(input.ids("ID авторов через запятую: "));
        int copies = input.number("Всего экземпляров: ", 1, 10000);
        return new Book(id, isbn, title, year, category, copies, authors);
    }

    private void readerMenu() {
        while (true) {
            out.println("\nЧИТАТЕЛИ: 1. Все  2. По ID  3. Добавить  4. Изменить  5. Удалить  6. Поиск  0. Назад");
            int choice = input.number("> ", 0, 6);
            if (choice == 0) return;
            safely(() -> {
                switch (choice) {
                    case 1 -> print(readers.findAll());
                    case 2 -> {
                        Person person = readers.findById(input.id("ID читателя: ")).orElseThrow(() -> new LibraryException("Читатель не найден."));
                        out.println(person); // Person.toString() calls Reader.getRole() polymorphically.
                    }
                    case 3 -> out.println("Добавлено: " + readers.create(readReader(0)));
                    case 4 -> {
                        long id = input.id("ID читателя: ");
                        out.println(readers.findById(id).orElseThrow(() -> new LibraryException("Читатель не найден.")));
                        readers.update(readReader(id)); out.println("Читатель изменён.");
                    }
                    case 5 -> {
                        long id = input.id("ID читателя: ");
                        if (input.confirm("Удалить читателя #" + id + "?")) { readers.delete(id); out.println("Читатель удалён."); }
                    }
                    case 6 -> print(readers.search(input.text("Имя или email: ", 160)));
                    default -> throw new IllegalStateException();
                }
            });
        }
    }

    private Reader readReader(long id) {
        String name = input.text("ФИО: ", 120);
        String email = input.text("Email: ", 160);
        int type = input.number("Тип: 1. STUDENT (5 книг / 30 дней)  2. GUEST (2 книги / 14 дней): ", 1, 2);
        return new Reader(id, name, email, type == 1 ? MembershipType.STUDENT : MembershipType.GUEST);
    }

    private void loanMenu() {
        while (true) {
            out.println("\nВЫДАЧИ: 1. Вся история  2. На руках  3. Выдать  4. Вернуть  0. Назад");
            int choice = input.number("> ", 0, 4);
            if (choice == 0) return;
            safely(() -> {
                switch (choice) {
                    case 1 -> print(loans.findAll(false)); case 2 -> print(loans.findAll(true));
                    case 3 -> {
                        long bookId = input.id("ID книги: "); long readerId = input.id("ID читателя: ");
                        out.println("Книга выдана. ID выдачи: " + lending.issue(bookId, readerId));
                    }
                    case 4 -> { lending.returnBook(input.id("ID выдачи: ")); out.println("Книга возвращена."); }
                    default -> throw new IllegalStateException();
                }
            });
        }
    }

    private void catalogMenu() {
        while (true) {
            out.println("\nСПРАВОЧНИКИ: 1. Авторы  2. Добавить автора  3. Категории  4. Добавить категорию  0. Назад");
            int choice = input.number("> ", 0, 4);
            if (choice == 0) return;
            safely(() -> {
                switch (choice) {
                    case 1 -> print(catalog.findAuthors()); case 2 -> out.println(catalog.createAuthor(input.text("Автор: ", 120)));
                    case 3 -> print(catalog.findCategories()); case 4 -> out.println(catalog.createCategory(input.text("Категория: ", 80)));
                    default -> throw new IllegalStateException();
                }
            });
        }
    }

    private void reportMenu() {
        while (true) {
            out.println("\nОТЧЁТЫ: 1. Популярность книг  2. Статистика категорий  3. Просрочки  0. Назад");
            int choice = input.number("> ", 0, 3);
            if (choice == 0) return;
            safely(() -> {
                switch (choice) {
                    case 1 -> print(reports.popularBooks()); case 2 -> print(reports.categoryStats());
                    case 3 -> print(reports.overdue(LocalDate.now()));
                    default -> throw new IllegalStateException();
                }
            });
        }
    }

    private void print(List<?> items) {
        if (items.isEmpty()) out.println("Записей нет."); else items.forEach(out::println);
    }

    private void safely(Runnable action) {
        try { action.run(); }
        catch (LibraryException | IllegalArgumentException e) { out.println("Ошибка: " + e.getMessage()); }
    }
}

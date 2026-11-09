package kg.alatoo.library;

import kg.alatoo.library.config.DatabaseConfig;
import kg.alatoo.library.dao.*;
import kg.alatoo.library.db.Database;
import kg.alatoo.library.db.SchemaInitializer;
import kg.alatoo.library.exception.LibraryException;
import kg.alatoo.library.model.MembershipType;
import kg.alatoo.library.service.*;
import kg.alatoo.library.ui.ConsoleInput;
import kg.alatoo.library.ui.ConsoleMenu;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    private static int run(String[] args) {
        if (Arrays.asList(args).contains("--help")) {
            System.out.println("Library Management: Java 17 + PostgreSQL\n"
                    + "Без аргументов: меню\n--init: создать схему\n--seed: добавить демонстрационные данные\n"
                    + "--init --seed: оба действия (без запуска меню)\n"
                    + "Подключение: db.properties либо DB_URL / DB_USER / DB_PASSWORD.");
            return 0;
        }
        if (Arrays.stream(args).anyMatch(a -> !a.equals("--init") && !a.equals("--seed"))) {
            System.err.println("Неизвестный аргумент. Используйте --help."); return 1;
        }
        try {
            Database database = new Database(DatabaseConfig.load());
            SchemaInitializer initializer = new SchemaInitializer(database);
            if (Arrays.asList(args).contains("--init")) { initializer.initialize(); System.out.println("Схема library создана или уже существует."); }
            if (Arrays.asList(args).contains("--seed")) { initializer.seed(); System.out.println("Демонстрационные данные добавлены."); }
            if (args.length > 0) return 0;
            BookDao books = new JdbcBookDao(database);
            ReaderDao readers = new JdbcReaderDao(database);
            LoanDao loans = new JdbcLoanDao(database);
            // Fail early with a helpful message if the schema or connection is missing.
            readers.findAll();
            LoanService lending = new LoanService(readers, loans, Map.of(
                    MembershipType.STUDENT, new StudentLendingPolicy(), MembershipType.GUEST, new GuestLendingPolicy()), Clock.systemDefaultZone());
            try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
                new ConsoleMenu(new ConsoleInput(scanner, System.out), System.out, books, readers,
                        new JdbcCatalogDao(database), loans, lending, new JdbcReportDao(database)).run();
            }
            return 0;
        } catch (LibraryException e) { System.err.println("Ошибка: " + e.getMessage()); return 1; }
    }
}

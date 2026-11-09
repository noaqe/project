package kg.alatoo.library.ui;

import kg.alatoo.library.util.Validation;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Always reads a complete line, avoiding Scanner.nextInt()/nextLine() newline problems. */
public final class ConsoleInput {
    public static final class EndOfInput extends RuntimeException { }
    private final Scanner scanner;
    private final PrintStream out;
    public ConsoleInput(Scanner scanner, PrintStream out) { this.scanner = scanner; this.out = out; }

    public String line(String prompt) {
        out.print(prompt); out.flush();
        if (!scanner.hasNextLine()) throw new EndOfInput();
        return scanner.nextLine().trim();
    }

    public String text(String prompt, int maxLength) {
        while (true) {
            try { return Validation.text(line(prompt), "Ввод", maxLength); }
            catch (IllegalArgumentException e) { out.println(e.getMessage()); }
        }
    }

    public long id(String prompt) {
        while (true) {
            try { return Validation.positiveId(Long.parseLong(line(prompt))); }
            catch (IllegalArgumentException e) { out.println("Введите положительный целый ID."); }
        }
    }

    public int number(String prompt, int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(line(prompt));
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) { }
            out.println("Введите целое число от " + min + " до " + max + ".");
        }
    }

    public List<Long> ids(String prompt) {
        while (true) {
            try {
                List<Long> ids = new ArrayList<>();
                for (String part : line(prompt).split(",", -1)) ids.add(Validation.positiveId(Long.parseLong(part.trim())));
                if (ids.stream().distinct().count() != ids.size()) throw new IllegalArgumentException();
                return ids;
            } catch (IllegalArgumentException e) { out.println("Введите уникальные ID через запятую, например: 1,2."); }
        }
    }

    public boolean confirm(String description) { return line(description + " Введите YES для подтверждения: ").equals("YES"); }
}

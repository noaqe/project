package kg.alatoo.library.util;

import java.util.Locale;

/** Shared validation for both menu input and programmatically constructed objects. */
public final class Validation {
    private Validation() { }

    public static String text(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + ": значение не должно быть пустым.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field + ": максимум " + maxLength + " символов, без управляющих символов.");
        }
        return normalized;
    }

    public static long positiveId(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID должен быть положительным.");
        return id;
    }

    public static String email(String value) {
        String normalized = text(value, "Email", 160).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Некорректный email. Пример: aida@example.com");
        }
        return normalized;
    }

    public static String isbn(String value) {
        if (value == null) throw new IllegalArgumentException("ISBN обязателен.");
        String digits = value.replace("-", "").replace(" ", "");
        if (!digits.matches("[0-9]{13}")) throw new IllegalArgumentException("ISBN должен содержать 13 цифр.");
        int sum = 0;
        for (int i = 0; i < 13; i++) sum += (digits.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        if (sum % 10 != 0) throw new IllegalArgumentException("Неверная контрольная цифра ISBN-13.");
        return digits;
    }
}

package kg.alatoo.library.model;

import kg.alatoo.library.util.Validation;

public record Category(long id, String name) {
    public Category {
        if (id < 0) throw new IllegalArgumentException("ID не может быть отрицательным.");
        name = Validation.text(name, "Категория", 80);
    }

    @Override public String toString() { return id + " | " + name; }
}

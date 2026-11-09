package kg.alatoo.library.model;

import kg.alatoo.library.util.Validation;

public record Author(long id, String name) {
    public Author {
        if (id < 0) throw new IllegalArgumentException("ID не может быть отрицательным.");
        name = Validation.text(name, "Автор", 120);
    }

    @Override public String toString() { return id + " | " + name; }
}

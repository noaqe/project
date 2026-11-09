package kg.alatoo.library.model;

import kg.alatoo.library.util.Validation;

/** Abstract base for people; the menu can display a Reader through a Person reference. */
public abstract class Person {
    private final long id;
    private final String fullName;
    private final String email;

    protected Person(long id, String fullName, String email) {
        if (id < 0) throw new IllegalArgumentException("ID не может быть отрицательным.");
        this.id = id;
        this.fullName = Validation.text(fullName, "ФИО", 120);
        this.email = Validation.email(email);
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public abstract String getRole();

    @Override public String toString() {
        return id + " | " + fullName + " | " + email + " | " + getRole();
    }
}

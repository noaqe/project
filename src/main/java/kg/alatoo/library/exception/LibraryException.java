package kg.alatoo.library.exception;

/** An expected business or database error suitable for display in the menu. */
public class LibraryException extends RuntimeException {
    public LibraryException(String message) { super(message); }
    public LibraryException(String message, Throwable cause) { super(message, cause); }
}

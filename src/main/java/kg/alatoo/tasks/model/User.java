package kg.alatoo.tasks.model;
/** Неизменяемые данные, которые Spring преобразует в JSON. */
public record User(long id, String name, String email) {}

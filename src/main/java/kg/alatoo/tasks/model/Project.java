package kg.alatoo.tasks.model;
/** Неизменяемые данные, которые Spring преобразует в JSON. */
public record Project(long id, String name, String description) {}

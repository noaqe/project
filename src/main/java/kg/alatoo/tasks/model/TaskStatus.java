package kg.alatoo.tasks.model;
/** Неизменяемые данные, которые Spring преобразует в JSON. */
public record TaskStatus(long id, String code, String name) {}

package kg.alatoo.tasks.model;
/** Неизменяемые данные, которые Spring преобразует в JSON. */
public record Comment(long id, long taskId, long authorId, String authorName, String body, java.time.OffsetDateTime createdAt) {}

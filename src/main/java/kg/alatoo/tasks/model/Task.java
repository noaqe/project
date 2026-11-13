package kg.alatoo.tasks.model;
/** Неизменяемые данные, которые Spring преобразует в JSON. */
public record Task(long id, String title, String description, long projectId, String projectName, long assigneeId, String assigneeName, long statusId, String statusCode, String statusName, java.time.LocalDate dueDate, java.time.OffsetDateTime createdAt) {}

package kg.alatoo.tasks.model;
/** Неизменяемые данные, которые Spring преобразует в JSON. */
public record ProjectReport(long projectId, String projectName, long totalTasks, long completedTasks) {}

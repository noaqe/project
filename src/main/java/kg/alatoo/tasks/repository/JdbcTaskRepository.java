package kg.alatoo.tasks.repository;
import kg.alatoo.tasks.dto.TaskRequest;
import kg.alatoo.tasks.model.Task;
import java.util.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTaskRepository implements TaskRepository {
    // JOIN добавляет в ответ названия, но в самой таблице tasks хранятся только ID.
    private static final String SELECT = """
        SELECT t.*, p.name AS project_name, u.name AS assignee_name,
               s.code AS status_code, s.name AS status_name
        FROM tasks t
        JOIN projects p ON p.id = t.project_id
        JOIN app_users u ON u.id = t.assignee_id
        JOIN task_statuses s ON s.id = t.status_id
        """;
    private final JdbcTemplate jdbc;
    private final RowMapper<Task> mapper = (rs, row) -> new Task(
        rs.getLong("id"), rs.getString("title"), rs.getString("description"),
        rs.getLong("project_id"), rs.getString("project_name"),
        rs.getLong("assignee_id"), rs.getString("assignee_name"),
        rs.getLong("status_id"), rs.getString("status_code"), rs.getString("status_name"),
        rs.getObject("due_date", LocalDate.class), rs.getObject("created_at", OffsetDateTime.class));
    public JdbcTaskRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Task> findAll(String search, Long projectId, Long statusId) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND (t.title ILIKE ? ESCAPE '!' OR t.description ILIKE ? ESCAPE '!')");
            String pattern = "%" + search.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            args.add(pattern); args.add(pattern);
        }
        if (projectId != null) { sql.append(" AND t.project_id = ?"); args.add(projectId); }
        if (statusId != null) { sql.append(" AND t.status_id = ?"); args.add(statusId); }
        sql.append(" ORDER BY t.id");
        return jdbc.query(sql.toString(), mapper, args.toArray());
    }
    public Optional<Task> findById(long id) {
        return jdbc.query(SELECT + " WHERE t.id = ?", mapper, id).stream().findFirst();
    }
    public long create(TaskRequest r) {
        return jdbc.queryForObject("""
            INSERT INTO tasks(title, description, project_id, assignee_id, status_id, due_date)
            VALUES (?, ?, ?, ?, ?, ?) RETURNING id
            """, Long.class, r.title().trim(), description(r), r.projectId(), r.assigneeId(), r.statusId(), r.dueDate());
    }
    public boolean update(long id, TaskRequest r) {
        return jdbc.update("""
            UPDATE tasks SET title = ?, description = ?, project_id = ?, assignee_id = ?, status_id = ?, due_date = ?
            WHERE id = ?
            """, r.title().trim(), description(r), r.projectId(), r.assigneeId(), r.statusId(), r.dueDate(), id) > 0;
    }
    public boolean delete(long id) { return jdbc.update("DELETE FROM tasks WHERE id = ?", id) > 0; }
    private String description(TaskRequest r) { return r.description() == null ? "" : r.description(); }
}

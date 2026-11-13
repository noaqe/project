package kg.alatoo.tasks.repository;
import kg.alatoo.tasks.model.*;
import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
    private final JdbcTemplate jdbc;
    public CatalogRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public boolean userExists(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app_users WHERE id = ?)", Boolean.class, id));
    }
    public boolean statusExists(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM task_statuses WHERE id = ?)", Boolean.class, id));
    }
    public List<User> users() {
        return jdbc.query("SELECT * FROM app_users ORDER BY id", (rs, n) ->
            new User(rs.getLong("id"), rs.getString("name"), rs.getString("email")));
    }
    public List<TaskStatus> statuses() {
        return jdbc.query("SELECT * FROM task_statuses ORDER BY id", (rs, n) ->
            new TaskStatus(rs.getLong("id"), rs.getString("code"), rs.getString("name")));
    }
    public List<Comment> comments(long taskId) {
        return jdbc.query("""
            SELECT c.*, u.name AS author_name FROM comments c
            JOIN app_users u ON u.id = c.author_id WHERE c.task_id = ? ORDER BY c.id
            """, (rs, n) -> new Comment(rs.getLong("id"), rs.getLong("task_id"), rs.getLong("author_id"),
                rs.getString("author_name"), rs.getString("body"), rs.getObject("created_at", OffsetDateTime.class)), taskId);
    }
}

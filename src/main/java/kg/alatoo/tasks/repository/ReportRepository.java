package kg.alatoo.tasks.repository;
import kg.alatoo.tasks.model.ProjectReport;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository {
    private final JdbcTemplate jdbc;
    public ReportRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<ProjectReport> byProject() {
        return jdbc.query("""
            SELECT p.id, p.name, COUNT(t.id) AS total_tasks,
                   COUNT(t.id) FILTER (WHERE s.code = 'DONE') AS completed_tasks
            FROM projects p
            LEFT JOIN tasks t ON t.project_id = p.id
            LEFT JOIN task_statuses s ON s.id = t.status_id
            GROUP BY p.id, p.name ORDER BY p.id
            """, (rs, row) -> new ProjectReport(rs.getLong("id"), rs.getString("name"),
                rs.getLong("total_tasks"), rs.getLong("completed_tasks")));
    }
}

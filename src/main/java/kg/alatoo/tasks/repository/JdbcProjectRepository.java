package kg.alatoo.tasks.repository;
import kg.alatoo.tasks.dto.ProjectRequest;
import kg.alatoo.tasks.model.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProjectRepository implements ProjectRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Project> mapper = (rs, row) ->
        new Project(rs.getLong("id"), rs.getString("name"), rs.getString("description"));
    public JdbcProjectRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Project> findAll() { return jdbc.query("SELECT * FROM projects ORDER BY id", mapper); }
    public Optional<Project> findById(long id) {
        return jdbc.query("SELECT * FROM projects WHERE id = ?", mapper, id).stream().findFirst();
    }
    public long create(ProjectRequest r) {
        return jdbc.queryForObject("INSERT INTO projects(name, description) VALUES (?, ?) RETURNING id",
            Long.class, r.name().trim(), r.description() == null ? "" : r.description());
    }
    public boolean update(long id, ProjectRequest r) {
        return jdbc.update("UPDATE projects SET name = ?, description = ? WHERE id = ?",
            r.name().trim(), r.description() == null ? "" : r.description(), id) > 0;
    }
    public boolean delete(long id) { return jdbc.update("DELETE FROM projects WHERE id = ?", id) > 0; }
}

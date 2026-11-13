package kg.alatoo.tasks.repository;
import kg.alatoo.tasks.dto.ProjectRequest;
import kg.alatoo.tasks.model.Project;
import java.util.List;
import java.util.Optional;
public interface ProjectRepository {
    List<Project> findAll();
    Optional<Project> findById(long id);
    long create(ProjectRequest request);
    boolean update(long id, ProjectRequest request);
    boolean delete(long id);
}

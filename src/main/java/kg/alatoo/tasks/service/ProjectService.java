package kg.alatoo.tasks.service;
import kg.alatoo.tasks.dto.ProjectRequest;
import kg.alatoo.tasks.exception.NotFoundException;
import kg.alatoo.tasks.model.Project;
import kg.alatoo.tasks.repository.ProjectRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    public ProjectService(ProjectRepository projects) { this.projects = projects; }
    public List<Project> findAll() { return projects.findAll(); }
    public Project get(long id) {
        return projects.findById(id).orElseThrow(() -> new NotFoundException("Проект не найден"));
    }
    @Transactional
    public Project create(ProjectRequest r) { return get(projects.create(r)); }
    @Transactional
    public Project update(long id, ProjectRequest r) {
        if (!projects.update(id, r)) throw new NotFoundException("Проект не найден");
        return get(id);
    }
    public void delete(long id) {
        if (!projects.delete(id)) throw new NotFoundException("Проект не найден");
    }
}

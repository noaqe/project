package kg.alatoo.tasks.service;
import kg.alatoo.tasks.dto.TaskRequest;
import kg.alatoo.tasks.exception.NotFoundException;
import kg.alatoo.tasks.model.*;
import kg.alatoo.tasks.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectService projects;
    private final CatalogRepository catalog;
    public TaskService(TaskRepository tasks, ProjectService projects, CatalogRepository catalog) {
        this.tasks = tasks; this.projects = projects; this.catalog = catalog;
    }
    public List<Task> findAll(String search, Long projectId, Long statusId) {
        return tasks.findAll(search, projectId, statusId);
    }
    public Task get(long id) {
        return tasks.findById(id).orElseThrow(() -> new NotFoundException("Задача не найдена"));
    }
    @Transactional
    public Task create(TaskRequest r) { checkReferences(r); return get(tasks.create(r)); }
    @Transactional
    public Task update(long id, TaskRequest r) {
        get(id); checkReferences(r);
        if (!tasks.update(id, r)) throw new NotFoundException("Задача не найдена");
        return get(id);
    }
    public void delete(long id) {
        if (!tasks.delete(id)) throw new NotFoundException("Задача не найдена");
    }
    public List<Comment> comments(long id) { get(id); return catalog.comments(id); }
    private void checkReferences(TaskRequest r) {
        projects.get(r.projectId());
        if (!catalog.userExists(r.assigneeId())) throw new NotFoundException("Исполнитель не найден");
        if (!catalog.statusExists(r.statusId())) throw new NotFoundException("Статус не найден");
    }
}

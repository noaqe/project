package kg.alatoo.tasks.repository;
import kg.alatoo.tasks.dto.TaskRequest;
import kg.alatoo.tasks.model.Task;
import java.util.List;
import java.util.Optional;
public interface TaskRepository {
    List<Task> findAll(String search, Long projectId, Long statusId);
    Optional<Task> findById(long id);
    long create(TaskRequest request);
    boolean update(long id, TaskRequest request);
    boolean delete(long id);
}

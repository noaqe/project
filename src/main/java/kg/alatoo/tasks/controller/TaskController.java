package kg.alatoo.tasks.controller;
import kg.alatoo.tasks.dto.TaskRequest;
import kg.alatoo.tasks.model.Task;
import kg.alatoo.tasks.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;
    public TaskController(TaskService service) { this.service = service; }
    @GetMapping({"", "/"})
    public List<Task> list(@RequestParam(required = false) String search,
                           @RequestParam(required = false) Long projectId,
                           @RequestParam(required = false) Long statusId) {
        return service.findAll(search, projectId, statusId);
    }
    @GetMapping({"/{id}", "/{id}/"})
    public Task get(@PathVariable long id) { return service.get(id); }
    @PostMapping({"", "/"})
    public ResponseEntity<Task> create(@Valid @RequestBody TaskRequest request) {
        Task result = service.create(request);
        return ResponseEntity.created(URI.create("/api/tasks/" + result.id())).body(result);
    }
    @PutMapping({"/{id}", "/{id}/"})
    public Task update(@PathVariable long id, @Valid @RequestBody TaskRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping({"/{id}", "/{id}/"})
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
    @GetMapping({"/{id}/comments", "/{id}/comments/"})
    public List<kg.alatoo.tasks.model.Comment> comments(@PathVariable long id) { return service.comments(id); }
}

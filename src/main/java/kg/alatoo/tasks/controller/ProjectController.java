package kg.alatoo.tasks.controller;
import kg.alatoo.tasks.dto.ProjectRequest;
import kg.alatoo.tasks.model.Project;
import kg.alatoo.tasks.service.ProjectService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;
    public ProjectController(ProjectService service) { this.service = service; }
    @GetMapping({"", "/"})
    public List<Project> list() { return service.findAll(); }
    @GetMapping({"/{id}", "/{id}/"})
    public Project get(@PathVariable long id) { return service.get(id); }
    @PostMapping({"", "/"})
    public ResponseEntity<Project> create(@Valid @RequestBody ProjectRequest request) {
        Project result = service.create(request);
        return ResponseEntity.created(URI.create("/api/projects/" + result.id())).body(result);
    }
    @PutMapping({"/{id}", "/{id}/"})
    public Project update(@PathVariable long id, @Valid @RequestBody ProjectRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping({"/{id}", "/{id}/"})
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
    
}

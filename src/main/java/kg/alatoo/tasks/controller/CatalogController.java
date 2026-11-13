package kg.alatoo.tasks.controller;
import kg.alatoo.tasks.model.*;
import kg.alatoo.tasks.repository.CatalogRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogRepository catalog;
    public CatalogController(CatalogRepository catalog) { this.catalog = catalog; }
    @GetMapping({"/users", "/users/"})
    public List<User> users() { return catalog.users(); }
    @GetMapping({"/statuses", "/statuses/"})
    public List<TaskStatus> statuses() { return catalog.statuses(); }
}

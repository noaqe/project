package kg.alatoo.tasks.controller;
import kg.alatoo.tasks.model.ProjectReport;
import kg.alatoo.tasks.repository.ReportRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportRepository reports;
    public ReportController(ReportRepository reports) { this.reports = reports; }
    @GetMapping({"/projects", "/projects/"})
    public List<ProjectReport> projects() { return reports.byProject(); }
}

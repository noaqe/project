package kg.alatoo.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIT {
    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url = System.getenv("TEST_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/[a-zA-Z0-9_]+_test")) {
            throw new IllegalStateException("Укажите TEST_DB_URL для отдельной локальной БД с суффиксом _test");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USER", "tasks"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", "tasks_demo_password"));
    }
    private long createProject() throws Exception {
        var response = api.perform(post("/api/projects/").contentType("application/json")
            .content("""
                {"name":"Тестовый проект","description":"CRUD"}
                """))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    }
    private String task(long projectId, String title) {
        return """
            {"title":"%s","description":"Тест","projectId":%d,"assigneeId":1,"statusId":1,"dueDate":"2027-01-10"}
            """.formatted(title, projectId);
    }
    @Test void seedAndTrailingSlash() throws Exception {
        for (String path : new String[]{"/api/tasks", "/api/tasks/"}) {
            api.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].projectName").value("Курсовая работа"));
        }
        api.perform(get("/api/users/")).andExpect(jsonPath("$", hasSize(3)));
        api.perform(get("/api/statuses/")).andExpect(jsonPath("$", hasSize(3)));
        api.perform(get("/api/tasks/1/comments")).andExpect(jsonPath("$[0].authorName").value("Тимур"));
    }
    @Test void projectCrud() throws Exception {
        long id = createProject();
        api.perform(get("/api/projects/" + id)).andExpect(status().isOk());
        api.perform(put("/api/projects/" + id).contentType("application/json")
            .content("""
                {"name":"Новое имя"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Новое имя"));
        api.perform(delete("/api/projects/" + id)).andExpect(status().isNoContent());
        api.perform(get("/api/projects/" + id)).andExpect(status().isNotFound());
    }
    @Test void taskCrud() throws Exception {
        long project = createProject();
        var response = api.perform(post("/api/tasks/").contentType("application/json").content(task(project, "Новая задача")))
            .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        api.perform(get("/api/tasks/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.assigneeName").value("Алина"));
        api.perform(put("/api/tasks/" + id).contentType("application/json").content(task(project, "Изменена")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Изменена"));
        api.perform(delete("/api/tasks/" + id)).andExpect(status().isNoContent());
        api.perform(get("/api/tasks/" + id)).andExpect(status().isNotFound());
        api.perform(delete("/api/projects/" + project)).andExpect(status().isNoContent());
    }
    @Test void searchAndFilters() throws Exception {
        api.perform(get("/api/tasks/").param("search", "join"))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(4));
        api.perform(get("/api/tasks").param("projectId", "1").param("statusId", "3"))
            .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(1));
        api.perform(get("/api/tasks").param("search", "%")).andExpect(jsonPath("$", hasSize(0)));
        api.perform(get("/api/tasks").param("search", "' OR 1=1 --")).andExpect(jsonPath("$", hasSize(0)));
    }
    @Test void reportIncludesEmptyProject() throws Exception {
        api.perform(get("/api/reports/projects/")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalTasks").value(3)).andExpect(jsonPath("$[0].completedTasks").value(1))
            .andExpect(jsonPath("$[2].totalTasks").value(0));
    }
    @Test void validationAndMissingReferences() throws Exception {
        api.perform(post("/api/tasks").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.title").exists());
        api.perform(post("/api/projects").contentType("application/json").content("""
            {"name":"   "}
            """))
            .andExpect(status().isBadRequest());
        api.perform(post("/api/tasks").contentType("application/json").content(task(999999, "Test")))
            .andExpect(status().isNotFound());
        api.perform(get("/api/tasks/abc")).andExpect(status().isBadRequest());
        api.perform(post("/api/tasks").contentType("application/json").content("broken"))
            .andExpect(status().isBadRequest());
        api.perform(get("/api/tasks/999999")).andExpect(status().isNotFound());
    }
    @Test void cannotDeleteProjectWithTasks() throws Exception {
        api.perform(delete("/api/projects/1")).andExpect(status().isConflict());
    }
    @Test void taskDeletionCascadesComments() throws Exception {
        api.perform(delete("/api/tasks/1")).andExpect(status().isNoContent());
        api.perform(get("/api/tasks/1/comments")).andExpect(status().isNotFound());
    }
}

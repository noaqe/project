# Диаграммы

GitHub отображает эти диаграммы прямо в браузере, в том числе на телефоне.

## ER

```mermaid
erDiagram
    APP_USERS ||--o{ TASKS : assigned
    PROJECTS ||--o{ TASKS : contains
    TASK_STATUSES ||--o{ TASKS : defines
    TASKS ||--o{ COMMENTS : contains
    APP_USERS ||--o{ COMMENTS : writes
    APP_USERS {
        bigint id PK
        varchar name
        varchar email UK
    }
    PROJECTS {
        bigint id PK
        varchar name
        varchar description
    }
    TASK_STATUSES {
        bigint id PK
        varchar code UK
        varchar name
    }
    TASKS {
        bigint id PK
        varchar title
        varchar description
        bigint project_id FK
        bigint assignee_id FK
        bigint status_id FK
        date due_date
        timestamptz created_at
    }
    COMMENTS {
        bigint id PK
        bigint task_id FK
        bigint author_id FK
        varchar body
        timestamptz created_at
    }
```

Удаление задачи каскадно удаляет комментарии. Удаление связанных проекта, пользователя или статуса ограничено FK.

## Основные классы

```mermaid
classDiagram
    class TaskController {
        -TaskService service
        +list()
        +get()
        +create()
        +update()
        +delete()
    }
    class TaskService {
        -TaskRepository tasks
        +get()
        +create()
        +update()
        +delete()
        -checkReferences()
    }
    class TaskRepository {
        <<interface>>
        +findAll()
        +findById()
        +create()
        +update()
        +delete()
    }
    class JdbcTaskRepository {
        -JdbcTemplate jdbc
    }
    class ProjectController {
        -ProjectService service
        +list()
        +get()
        +create()
        +update()
        +delete()
    }
    class ProjectService {
        -ProjectRepository projects
        +get()
        +create()
        +update()
        +delete()
    }
    class ProjectRepository {
        <<interface>>
        +findAll()
        +findById()
        +create()
        +update()
        +delete()
    }
    class JdbcProjectRepository {
        -JdbcTemplate jdbc
    }
    class CatalogRepository {
        +users()
        +statuses()
        +comments()
        +userExists()
        +statusExists()
    }
    class ReportController {
        +projects()
    }
    class ReportRepository {
        +byProject()
    }
    class Task {
        <<record>>
        +long id
        +String title
        +long projectId
        +long assigneeId
        +long statusId
    }
    class Project {
        <<record>>
        +long id
        +String name
        +String description
    }
    TaskController --> TaskService
    TaskService --> TaskRepository
    TaskRepository <|.. JdbcTaskRepository
    TaskService --> ProjectService
    TaskService --> CatalogRepository
    ProjectController --> ProjectService
    ProjectService --> ProjectRepository
    ProjectRepository <|.. JdbcProjectRepository
    ReportController --> ReportRepository
    TaskRepository ..> Task
    ProjectRepository ..> Project
```

Показаны основные зависимости. DTO, вспомогательные модели и часть полей Task опущены для читаемости.

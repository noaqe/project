# Что показывать преподавателю в IntelliJ

Дважды Shift — поиск класса. Запустите TaskTrackerApplication.

1. **TaskTrackerApplication:** main и @SpringBootApplication запускают Spring и HTTP-сервер.
2. **Браузер /api/tasks/:** показать JSON из PostgreSQL.
3. **TaskController:** @RestController, @GetMapping, @PostMapping. @RequestBody читает JSON, @Valid запускает проверки.
4. **TaskRequest:** @NotBlank — обязательное название, @Size — длина, @NotNull и @Positive — ID.
5. **TaskService:** create и checkReferences проверяют связи; @Transactional задаёт транзакцию.
6. **TaskRepository → JdbcTaskRepository:** интерфейс и реализация. Показать INSERT с ?, bind-параметры, RowMapper и JOIN.
7. **V1__schema.sql и ER:** пять таблиц, PK, FK, 1:N, RESTRICT и CASCADE.
8. **ReportRepository → /api/reports/projects/:** LEFT JOIN, COUNT(t.id), FILTER, GROUP BY.
9. **ProjectController:** CRUD второй сущности.
10. **Dockerfile и compose.yaml:** сборка JAR, две службы, постоянное хранение БД.

## Демонстрация CRUD

Через Postman или requests.http создайте проект, запомните ID.
Создайте задачу в этом проекте. Покажите GET, измените через PUT, удалите через DELETE.
Удалите пустой проект. Удаление проекта с задачами возвращает 409.
Пустое название даёт 400, отсутствующий ID — 404.

## Короткие ответы

- База — PostgreSQL; tasks_db — имя базы. В ней пять таблиц приложения и техническая таблица Flyway.
- Данные сохраняются в PostgreSQL и Docker volume.
- JDBC позволяет явно показать SQL; JdbcTemplate упрощает открытие и закрытие ресурсов.
- Spring создаёт объекты-компоненты, внедряет зависимости, обрабатывает HTTP, JSON, валидацию и транзакции.
- record — неизменяемый носитель данных с автоматически созданными методами доступа, equals, hashCode и toString.
- Полиморфизм: TaskService использует TaskRepository, фактический объект — JdbcTaskRepository.
- 3NF: названия связанных сущностей не копируются в задачи.
- Flyway выполняет только новые миграции, поэтому стартовые данные не дублируются.

Поставьте breakpoint в TaskController.list(), запустите Debug и откройте /api/tasks/.
F8 — следующая строка, F7 — войти в метод, F9 — продолжить.

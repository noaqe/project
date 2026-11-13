# Task Tracker — учёт задач

Небольшой учебный REST backend: **Java 17 + Spring Boot 3.5 + PostgreSQL 17**.
Без frontend, Lombok и Hibernate. SQL написан явно, доступ к БД — через Spring JDBC (JdbcTemplate).

**[Скачать ZIP](https://github.com/noaqe/project/archive/refs/heads/task-tracker.zip)** ·
**[Короткий отчёт](docs/report.md)** · **[Защита в IntelliJ](docs/defense.md)** ·
**[ER и class diagram](docs/diagrams.md)**

Это отдельная ветка `task-tracker`. Библиотека осталась в `main`.
В этой GitHub-версии находятся исходники и документация; готовый JAR собирается Docker или Maven.

## Запуск Docker

Нужен Docker Desktop (Windows/macOS) или Docker Engine с Compose (Linux).
Распакуйте ZIP, откройте терминал в папке с compose.yaml:

```bash
docker compose up --build -d
```

Первый запуск скачивает образы и зависимости, собирает JAR и создаёт БД.
Откройте:

- http://localhost:8080/api/tasks/ — пять стартовых задач в JSON;
- http://localhost:8080/api/projects/ — три проекта;
- http://localhost:8080/api/users/ — три исполнителя;
- http://localhost:8080/api/statuses/ — три статуса;
- http://localhost:8080/api/tasks/1/comments/ — комментарии;
- http://localhost:8080/api/tasks/?search=JOIN — поиск;
- http://localhost:8080/api/tasks/?projectId=1&statusId=3 — фильтры;
- http://localhost:8080/api/reports/projects/ — отчёт JOIN + COUNT.

Поддерживаются адреса с завершающим / и без него. Корень / не содержит веб-страницу.
Логи: `docker compose logs -f app`. Остановка: `docker compose down`.
Данные сохраняются в volume. `docker compose down -v` удаляет данные — только для полного сброса учебной БД.

## IntelliJ IDEA

1. Нужен JDK 17+. Откройте pom.xml как Maven-проект. Project SDK и Maven Runner JRE должны указывать на JDK.
2. Запустите только PostgreSQL:
   ```bash
   docker compose -f compose.yaml -f compose.local.yaml up -d db
   ```
3. Если ранее запускали всё в Docker, освободите порт: `docker compose stop app`.
4. Запустите `src/main/java/kg/alatoo/tasks/TaskTrackerApplication.java`.
5. Откройте http://localhost:8080/api/tasks/.

Настройки по умолчанию: localhost:5433, база tasks_db, пользователь tasks, пароль tasks_demo_password.
Для своей БД задайте в Run Configuration переменные DB_URL, DB_USER, DB_PASSWORD.
Пример для обычного PostgreSQL: DB_URL=jdbc:postgresql://localhost:5432/tasks_db.
Базу и пользователя нужно создать заранее; таблицы создаёт Flyway.

## Сборка без Docker

Windows:
```powershell
.\mvnw.cmd clean package
java -jar target/task-tracker.jar
```

Linux/macOS:
```bash
chmod +x mvnw
./mvnw clean package
java -jar target/task-tracker.jar
```

Для приложения необходима работающая PostgreSQL. В Docker сборка выполняется автоматически.

## REST API

| Метод | Адрес | Действие |
|---|---|---|
| GET | /api/tasks | Список; search, projectId, statusId |
| GET | /api/tasks/{id} | Одна задача |
| POST | /api/tasks | Создать |
| PUT | /api/tasks/{id} | Заменить редактируемые поля |
| DELETE | /api/tasks/{id} | Удалить задачу и комментарии |
| GET | /api/projects | Все проекты |
| GET | /api/projects/{id} | Один проект |
| POST | /api/projects | Создать |
| PUT | /api/projects/{id} | Изменить |
| DELETE | /api/projects/{id} | Удалить пустой проект |
| GET | /api/users, /api/statuses | Справочники |
| GET | /api/tasks/{id}/comments | Комментарии |
| GET | /api/reports/projects | Число всех и завершённых задач |

POST возвращает 201 и Location, GET/PUT — 200, DELETE — 204 без тела.
Неверный ввод — 400, отсутствующая запись — 404, удаление проекта с задачами — 409.
Справочники и комментарии доступны для чтения.

Пример JSON для POST /api/tasks:
```json
{
  "title": "Подготовить защиту",
  "description": "Объяснить контроллер, сервис и SQL",
  "projectId": 1,
  "assigneeId": 1,
  "statusId": 1,
  "dueDate": "2027-01-10"
}
```

PUT использует такой же формат. title и три ID обязательны; description и dueDate — нет.
При PUT пропущенное описание становится пустым, срок — null.
Прошедшая дата разрешена, чтобы хранить просроченные задачи.
POST/PUT/DELETE выполняйте через Postman, curl или [requests.http](requests.http).
Адресная строка браузера делает GET.

## БД и стартовые данные

V1__schema.sql создаёт пять таблиц: app_users, projects, task_statuses, tasks, comments.
V2__demo_data.sql добавляет 3 пользователей, 3 проекта, 3 статуса, 5 задач и 3 комментария.
Flyway выполняет миграции один раз. Данные не дублируются при перезапуске.
Техническая flyway_schema_history хранит историю миграций.
Для действующей БД добавляйте V3, V4, а не редактируйте уже применённые файлы.

```bash
docker compose exec db psql -U tasks -d tasks_db -c "SELECT * FROM tasks;"
```

В DBeaver/pgAdmin подключайтесь через compose.local.yaml к localhost:5433.

## Сервер

Скопируйте проект на сервер с Docker Compose, создайте .env по .env.example и задайте свой пароль до первого запуска.
Выполните `docker compose up --build -d`.
Откройте http://SERVER_IP:8080/api/tasks/.
APP_PORT меняет внешний порт приложения. Основной compose не публикует PostgreSQL наружу.
Изменение пароля в .env не меняет пароль в уже созданном volume.

API учебный, без авторизации: любой с сетевым доступом может менять записи.
Для защиты ограничьте доступ к серверу доверенными пользователями; для публичного сервиса нужны авторизация и HTTPS.

## Тесты на PostgreSQL

Обычная сборка не запускает интеграционные тесты. Для них нужна отдельная локальная БД с суффиксом _test:

```bash
docker run -d --name task-tracker-test-db -e POSTGRES_DB=tasks_test -e POSTGRES_USER=tasks -e POSTGRES_PASSWORD=tasks_demo_password -p 127.0.0.1:15433:5432 postgres:17-alpine
# Дождаться готовности:
docker exec task-tracker-test-db pg_isready -U tasks -d tasks_test
TEST_DB_URL=jdbc:postgresql://localhost:15433/tasks_test ./mvnw -Pintegration verify
```

PowerShell: сначала `$env:TEST_DB_URL="jdbc:postgresql://localhost:15433/tasks_test"`,
затем `.\mvnw.cmd -Pintegration verify`.
При других доступах задайте TEST_DB_USER и TEST_DB_PASSWORD.
Изменения тестов откатываются; Flyway один раз создаёт тестовую схему и данные.
Не используйте рабочую БД.

## Требования курсовой

| Требование | Реализация |
|---|---|
| 4–7 связанных таблиц, 3NF | 5 таблиц приложения; объяснение в отчёте |
| CRUD двух сущностей | TaskController, ProjectController |
| Поиск, JOIN | JdbcTaskRepository.findAll |
| Отчёт, агрегаты | ReportRepository.byProject |
| Валидация | DTO, @Valid, сервис, ограничения БД |
| README, ER, class diagram | Этот файл и docs/diagrams.md |

Если преподаватель требует именно консольное меню из методички, согласуйте замену меню на REST API:
по заданию этот проект представляет собой backend с HTTP-маршрутами.

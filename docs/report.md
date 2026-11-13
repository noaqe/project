# Краткий отчёт: «Учёт задач»

## Цель и технологии

Backend хранит задачи, распределяет их по проектам и исполнителям, позволяет искать задачи и смотреть статистику.
Java 17, Spring Boot, PostgreSQL 17, Spring JDBC (JdbcTemplate), Maven и Docker Compose.
HTTP API возвращает JSON. PostgreSQL — СУБД, tasks_db — имя базы, SQL — язык запросов, JDBC — Java API для работы с БД.

## Устройство программы

Запрос проходит цепочку **Controller → Service → Repository → PostgreSQL**.
Контроллер принимает HTTP-запрос. Сервис проверяет связанные данные и задаёт границу транзакции.
Репозиторий выполняет SQL с параметрами и превращает строки в Java-объекты.
Spring создаёт компоненты и передаёт зависимости через конструкторы.
Для чтения справочников и отчёта контроллер обращается к репозиторию напрямую, поскольку дополнительных правил нет.

TaskTrackerApplication запускает приложение. TaskController и ProjectController реализуют CRUD.
TaskService и ProjectService содержат логику. JdbcTaskRepository и JdbcProjectRepository выполняют SQL.
ReportRepository строит отчёт. ApiExceptionHandler возвращает JSON-ошибки.
Модели Task, Project и другие — неизменяемые record. TaskRequest и ProjectRequest — DTO входного JSON.

## База данных

| Таблица | Содержание |
|---|---|
| app_users | id, name, email |
| projects | id, name, description |
| task_statuses | id, code, name |
| tasks | id, title, description, project_id, assignee_id, status_id, due_date, created_at |
| comments | id, task_id, author_id, body, created_at |

Связи 1:N: проект → задачи; пользователь → назначенные задачи; статус → задачи;
задача → комментарии; пользователь → комментарии.
PK уникально определяет строку. FK запрещает ссылки на несуществующие записи.
Проект с задачами удалить нельзя (RESTRICT). При удалении задачи её комментарии удаляются (CASCADE).
ID генерирует PostgreSQL.

**1NF:** атомарные значения, без списков в ячейках.
**2NF:** простые первичные ключи, нет зависимости от части составного ключа.
**3NF:** имена проекта, исполнителя и статуса находятся в собственных таблицах; задача хранит только ID.
При принятых правилах нет транзитивных зависимостей неключевых атрибутов от ключей.
Имена в JSON получены через JOIN, а не дублируются в tasks.

## Функции и SQL

Для задач и проектов реализованы создание, список, чтение по ID, изменение и удаление.
Поиск использует ILIKE по названию и описанию; фильтры — projectId и statusId.
Отчёт /api/reports/projects считает все и завершённые задачи:

```sql
SELECT p.id, p.name, COUNT(t.id) AS total_tasks,
       COUNT(t.id) FILTER (WHERE s.code = 'DONE') AS completed_tasks
FROM projects p
LEFT JOIN tasks t ON t.project_id = p.id
LEFT JOIN task_statuses s ON s.id = t.status_id
GROUP BY p.id, p.name
ORDER BY p.id;
```

LEFT JOIN сохраняет проекты без задач. COUNT(t.id) возвращает для них ноль; COUNT(*) посчитал бы строку соединения.
GROUP BY объединяет строки одной группы для подсчёта.

Валидация проверяет название, длины, обязательные положительные ID и формат даты.
Сервис проверяет существование проекта, пользователя и статуса.
Ошибки: 400 — ввод, 404 — запись не найдена, 409 — конфликт связей.
SQL-параметры передаются отдельно от текста запроса; % и _ экранируются для буквального поиска.

## ООП и Spring

Инкапсуляция — private final зависимости, неизменяемые record.
Абстракция — интерфейсы TaskRepository и ProjectRepository.
Полиморфизм — сервис вызывает интерфейс, фактическая реализация предоставлена Spring.
Наследование — NotFoundException extends RuntimeException.
HTTP, правила и SQL разделены между классами.
@Transactional объединяет операции в транзакцию, а необработанное runtime-исключение вызывает откат.
JdbcTemplate управляет JDBC-ресурсами; Hibernate здесь не используется.

## Запуск

Flyway создаёт структуру и стартовые данные двумя миграциями. Повторный запуск не дублирует данные.
Dockerfile сначала собирает JAR Maven, затем запускает его в Java 17 от непривилегированного пользователя.
Compose запускает PostgreSQL и приложение, ждёт готовности БД и хранит данные в volume.
После docker compose up --build -d адрес http://localhost:8080/api/tasks/ возвращает пять задач.

## Границы

Учебный backend без frontend, авторизации и пагинации. Справочники и комментарии доступны для чтения.
Полный CRUD — у задач и проектов. Доступ к серверу без авторизации следует ограничивать.

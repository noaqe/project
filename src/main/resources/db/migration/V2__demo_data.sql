INSERT INTO app_users(name, email) VALUES
 ('Алина', 'alina@example.com'), ('Тимур', 'timur@example.com'), ('Айбек', 'aibek@example.com');
INSERT INTO projects(name, description) VALUES
 ('Курсовая работа', 'Разработка backend для учёта задач'),
 ('Подготовка к экзаменам', 'Повторение учебного материала'),
 ('Личные дела', 'Проект пока без задач — для проверки LEFT JOIN');
INSERT INTO task_statuses(code, name) VALUES
 ('TODO', 'Новая'), ('IN_PROGRESS', 'В работе'), ('DONE', 'Завершена');
INSERT INTO tasks(title, description, project_id, assignee_id, status_id, due_date) VALUES
 ('Спроектировать базу данных', 'Создать пять таблиц и связи между ними', 1, 1, 3, CURRENT_DATE + 1),
 ('Реализовать REST API', 'Добавить CRUD задач и проектов', 1, 2, 2, CURRENT_DATE + 3),
 ('Подготовить Dockerfile', 'Проверить запуск на сервере', 1, 3, 1, CURRENT_DATE + 5),
 ('Повторить JOIN', 'Разобрать INNER JOIN и LEFT JOIN', 2, 1, 1, CURRENT_DATE + 2),
 ('Повторить ООП', 'Объяснить интерфейсы и инкапсуляцию', 2, 2, 3, CURRENT_DATE + 4);
INSERT INTO comments(task_id, author_id, body) VALUES
 (1, 2, 'Связи проверены, схема готова.'),
 (2, 1, 'Не забыть валидацию входных данных.'),
 (4, 3, 'Посмотрим отчёт по проектам.');

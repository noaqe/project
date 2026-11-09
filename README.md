# Library Management / Библиотека

Курсовой проект по **OOP and Databases**: консольная система учёта библиотеки на **Java 17**, **PostgreSQL** и **JDBC**. Открывается как Maven-проект в IntelliJ IDEA Community. Приложение работает с реальной БД, данные сохраняются между запусками.

## Возможности

- Полный CRUD книг и читателей: создание, список, просмотр по ID, изменение, удаление.
- У книги может быть несколько авторов; авторов и категории можно просматривать и добавлять из меню.
- Поиск книг по названию, ISBN или автору через JOIN/подзапрос; поиск читателей по имени и email.
- Выдача и возврат книг, вся история и список книг на руках.
- Отчёты: популярность книг (`LEFT JOIN`, `COUNT`, `GROUP BY`), статистика категорий (агрегаты), просрочки (JOIN трёх таблиц).
- Проверка ISBN-13 с контрольной цифрой, email, обязательных полей, диапазонов и существования связанных ID.
- Транзакции, `PreparedStatement`, ограничения БД и блокировки строк при выдаче.

## Соответствие заданию

| Требование | Реализация |
|---|---|
| 4–7 связанных таблиц | 6: `authors`, `categories`, `books`, `book_authors`, `readers`, `loans` |
| Нормализация до 3NF | [Объяснение зависимостей](docs/database.md); M:N через `book_authors` |
| Java-приложение с меню | `Main` → `ConsoleMenu` |
| CRUD минимум двух сущностей | `BookDao`, `ReaderDao` и все операции в меню |
| Поиск с JOIN | `JdbcBookDao.search()` |
| Отчёт с агрегатом | `JdbcReportDao.popularBooks()` и `categoryStats()` |
| Валидация | `Validation`, конструкторы моделей, `ConsoleInput`, ограничения SQL |
| ER + class diagram | [ER](docs/er-diagram.png), [UML](docs/class-diagram.png); исходники PlantUML рядом |
| ООП, DAO, слои | [Архитектура и объяснение ООП](docs/architecture.md) |
| JUnit 5 | Модульные и интеграционные тесты с PostgreSQL |

## Быстрый запуск в Windows / IntelliJ IDEA

Нужны **JDK 17 или новее** и PostgreSQL 17/18 **либо Docker Desktop**. Java 8 для проекта не подходит. Maven отдельно устанавливать не обязательно: в репозитории есть Maven Wrapper.

Исходный код: [github.com/noaqe/project](https://github.com/noaqe/project). Можно скачать ZIP через **Code → Download ZIP** либо выполнить `git clone https://github.com/noaqe/project.git`.

1. Откройте папку проекта или `pom.xml` в IntelliJ IDEA: **File → Open**.
2. В **File → Project Structure → Project SDK** выберите JDK 17+. Дождитесь загрузки Maven-зависимостей. Для Maven Runner тоже выберите этот JDK.
3. В терминале из корня проекта скопируйте пример настроек:

   ```powershell
   Copy-Item db.properties.example db.properties
   ```

4. Запустите PostgreSQL одним из способов ниже.
5. Соберите проект и создайте таблицы с демонстрационными данными:

   ```powershell
   .\mvnw.cmd clean package
   java -jar target/library-management.jar --init --seed
   java -jar target/library-management.jar
   ```

`--init` и `--seed` выполняют настройку и завершают работу. Для меню нужен отдельный запуск **без аргументов**. `--help` работает без подключения к БД.

Если `java -version` показывает Java 8, задайте `JAVA_HOME` на установленный JDK 17+ и добавьте его `bin` в начало `PATH` текущего терминала либо запускайте `Main` из IntelliJ с правильным SDK. Сам файл `mvnw.cmd` тоже использует `JAVA_HOME`.

В IntelliJ можно запускать класс `kg.alatoo.library.Main` зелёной кнопкой. **Working directory** должен указывать на корень проекта, где лежит `db.properties`. Для первого запуска укажите в **Program arguments** `--init --seed`, затем уберите аргументы.

### Вариант A: PostgreSQL через Docker

```powershell
docker compose up -d --wait db
```

По умолчанию БД доступна только с этого компьютера на порту **5433**, база `library_db`, пользователь `library_app`. Демонстрационный локальный пароль совпадает с `db.properties.example`. Данные сохраняются в Docker volume `library_data`.

Для своих значений скопируйте `.env.example` в `.env`, измените их и согласуйте с `db.properties`. Compose читает `.env`; Java читает `db.properties` или переменные `DB_*`. Остановка с сохранением данных: `docker compose down`. Изменение пароля в `.env` не меняет пароль уже созданного PostgreSQL volume.

### Вариант B: уже установленный PostgreSQL

В pgAdmin откройте Query Tool под администратором. Выполните **отдельными командами** (создание БД - вне транзакции):

```sql
CREATE USER library_app WITH PASSWORD 'library_local_password';
```

```sql
CREATE DATABASE library_db OWNER library_app;
```

Укажите в локальном `db.properties` фактический порт (обычно `5432`):

```properties
db.url=jdbc:postgresql://localhost:5432/library_db
db.user=library_app
db.password=library_local_password
```

Затем выполните команды сборки и инициализации выше. Пароль в примере предназначен для локальной демонстрации. Для отдельного сервера задайте собственные доступы в локальном файле или окружении.

### Linux / macOS

```bash
cp db.properties.example db.properties
chmod +x mvnw
docker compose up -d --wait db
./mvnw clean package
java -jar target/library-management.jar --init --seed
java -jar target/library-management.jar
```

При проблемах с русским текстом используйте терминал UTF-8. В старой Windows-консоли перед запуском можно выполнить `chcp 65001`. В IntelliJ: **Settings → Editor → File Encodings → UTF-8**.

## Настройка подключения

Переменные `DB_URL`, `DB_USER`, `DB_PASSWORD` имеют приоритет над `db.properties`. Пример адреса: `jdbc:postgresql://localhost:5433/library_db`. Адрес HTTP API не является адресом PostgreSQL; для БД нужны отдельные порт, имя БД и пользователь.

Файлы `db.properties` и `.env` исключены из Git. Доступы к чужим серверам не нужны для запуска этого проекта. `--init` создаёт схему `library` в выделенной БД, без `DROP`/`TRUNCATE`. Это начальная схема, а не система миграций.

## Как пользоваться меню

```text
1. Книги  2. Читатели  3. Выдачи  4. Справочники  5. Отчёты  0. Выход
```

В разделах книг и читателей есть все CRUD-операции. Изменение запрашивает все новые значения, перед этим показывает текущую запись. Для удаления нужно набрать `YES`. Для добавления книги сначала посмотрите ID категории и авторов в справочниках; нескольких авторов вводите через запятую.

Бизнес-правила:

- **STUDENT**: до 5 активных выдач, срок 30 дней; **GUEST**: до 2, срок 14 дней.
- Нельзя выдать книгу без свободного экземпляра, повторно выдать ту же книгу одному читателю или выдать новую при просрочке.
- В день возврата по сроку книга ещё не считается просроченной.
- Повторный возврат отклоняется. Нельзя уменьшить фонд ниже количества книг на руках.
- Книги и читатели с историей выдач не удаляются: FK сохраняет историю даже после возврата. Для демонстрации удаления создайте новую запись без выдач.
- Смена типа читателя влияет на новые выдачи; сроки уже созданных выдач сохраняются.
- Доступность вычисляется как `total_copies - активные выдачи`, отдельного дублирующего счётчика в БД нет.

`--seed` добавляет 4 книги, 4 авторов, 3 категории, 2 читателей и 3 примера выдач, включая просроченную. Повторный запуск не дублирует эти данные. Даты примеров рассчитываются относительно даты инициализации. Seed рассчитан на новую демонстрационную базу; для своей рабочей базы запускайте только `--init`.

## Проверки

Без БД:

```powershell
.\mvnw.cmd test
```

Интеграционные тесты проверяют CRUD, JOIN/M:N, откат транзакций, ограничения, отчёты, выдачу/возврат и параллельные запросы. Они сбрасывают **только схему `library` в отдельной локальной тестовой БД**. Для защиты от запуска по ошибке обязательны адрес `localhost`/`127.0.0.1`, имя БД с окончанием `_test` и явный `TEST_DB_RESET=true`. Параметры в тестовом JDBC URL не допускаются. Не используйте рабочую БД.

Создайте БД `library_test` с владельцем `library_app`, затем:

```powershell
$env:TEST_DB_URL="jdbc:postgresql://localhost:5433/library_test"
$env:TEST_DB_USER="library_app"
$env:TEST_DB_PASSWORD="library_local_password"
$env:TEST_DB_RESET="true"
.\mvnw.cmd -Pintegration verify
```

GitHub Actions запускает эти проверки на Java 17 и отдельном PostgreSQL 17; отчёты Maven находятся в `target/surefire-reports` и `target/failsafe-reports`. Проверенные локальные результаты: [docs/verification.md](docs/verification.md).

## Структура

```text
src/main/java/kg/alatoo/library/
  model/       объекты предметной области
  dao/         интерфейсы DAO и JDBC-реализации
  service/     правила и стратегии выдачи
  db/          подключения, транзакции, инициализация SQL
  config/      настройки из окружения / локального файла
  ui/          консольный ввод и меню
  util/        валидация
  exception/   понятные ошибки приложения
src/main/resources/db/   схема и demo seed
src/test/java/           JUnit 5
docs/                    ER, UML, 3NF, архитектура, сценарий защиты
```

[Сценарий защиты и разбор вопросов](docs/defense-guide.md). [Сдача ссылки преподавателю](docs/submission.md).

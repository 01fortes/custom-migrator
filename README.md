# Custom Migrator

Инструмент для очистки старых данных в PostgreSQL с минимальным downtime, используя технику shadow-таблиц.

## Описание

Custom Migrator позволяет безопасно удалять устаревшие данные из больших таблиц PostgreSQL без остановки приложения. Процесс работает следующим образом:

1. Создается теневая (shadow) таблица с актуальной структурой
2. Настраиваются триггеры для автоматической синхронизации новых данных
3. В shadow-таблицу копируются только актуальные данные (по временным окнам)
4. Выполняется swap: старая таблица заменяется на новую очищенную
5. Старые данные остаются в исходной таблице для архивации/удаления

Это позволяет освободить место в БД и улучшить производительность без простоя сервиса.

## Требования

- Java 21+
- PostgreSQL 12+
- Gradle 9.0.0 (используется wrapper)

## Конфигурация

### Базовые параметры (application.yaml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DATABASE}
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:password}

application:
  migration:
    start-at: "17:45"              # Время начала процесса очистки
    suspend-at: "17:44"            # Время остановки процесса
    window-step: "6 hours"         # Размер временного окна для копирования данных
    since-date: "2024-05-09T07:05:59"  # Копировать данные начиная с этой даты (старые - удаляются)
```

### Настройка миграции таблицы

Создайте YAML файл в `src/main/resources/migration/` (например, `message.yaml`):

```yaml
table:
  name: message
  shadow-name: message_shadow
  pk:
    columns:
      - id
    name: message_pkey
    shadow-name: message_shadow_pkey

  indexes:
    - name: idx_id
      shadow-name: idx_id_shadow
      columns: [id]

procedure:
  name: procedure_message_to_shadow

trigger:
  name: trigger_message_to_shadow
  procedure: procedure_message_to_shadow
```

Добавьте файл в `src/main/resources/migration/migration.yaml`:

```yaml
files:
  - message.yaml
  - your-table.yaml
```

## Сборка

```bash
./gradlew build
```

Артефакт будет создан в `build/libs/custom-migrator-{version}.jar`

## Запуск

### Локально

```bash
java -jar build/libs/custom-migrator-0.0.2.jar
```

### С переменными окружения

```bash
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DATABASE=mydb
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=secret
export CUSTOM_MIGRATION_START_AT="18:00"

java -jar build/libs/custom-migrator-0.0.2.jar
```

## API Endpoints

Приложение запускается на порту `8080` (по умолчанию).

### Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### Основные эндпоинты

- `POST /api/migration/execute-stage` - Выполнить определенный этап очистки
- `POST /api/migration/rollback` - Откатить этап (вернуть к предыдущему состоянию)
- `GET /api/migration/monitoring` - Мониторинг процесса в реальном времени (SSE)
- `GET /api/migration/check-all` - Проверить состояние всех процессов очистки
- `POST /api/migration/sql` - Выполнить произвольный SQL-запрос
- `GET /api/migration/fix/actions` - Получить список доступных действий для исправления
- `POST /api/migration/fix` - Выполнить исправление проблем

### Мониторинг и метрики

- Health: `http://localhost:13110/sys/health`
- Prometheus: `http://localhost:13110/sys/prometheus`
- Metrics: `http://localhost:13110/sys/metrics`

## Этапы процесса очистки

1. **CREATE_SHADOW_TABLE** - Создание теневой таблицы с той же структурой
2. **CREATE_MIRROR_TRIGGER** - Настройка триггера для синхронизации новых записей
3. **DATA_MIGRATION** - Копирование актуальных данных порциями (по временным окнам)
4. **SWAP** - Замена исходной таблицы на очищенную shadow-таблицу
5. **DELETE_MIRROR_FUNCTION** - Удаление временных триггеров и процедур

## Пример использования

```bash
# Выполнить этап создания shadow таблицы
curl -X POST http://localhost:8080/api/migration/execute-stage \
  -H "Content-Type: application/json" \
  -d '{"name": "message", "stage": "CREATE_SHADOW_TABLE"}'

# Откатить этап
curl -X POST http://localhost:8080/api/migration/rollback \
  -H "Content-Type: application/json" \
  -d '{"name": "message", "stage": "CREATE_SHADOW_TABLE"}'

# Проверить статус всех миграций
curl http://localhost:8080/api/migration/check-all
```

## Структура проекта

```
src/main/
├── kotlin/com/jsamkt/custom/migrator/
│   ├── controller/        # REST контроллеры
│   ├── service/          # Бизнес-логика и этапы миграции
│   ├── repository/       # Работа с БД
│   ├── entity/           # Модели данных
│   └── dto/              # DTO классы
└── resources/
    ├── application.yaml   # Конфигурация приложения
    ├── data.sql          # Схема служебных таблиц
    └── migration/        # YAML файлы миграций
```

## Технологии

- **Kotlin** 2.0.21
- **Spring Boot** 3.4.1
- **PostgreSQL** 42.7.8
- **Kotlin Coroutines** - асинхронная обработка
- **SpringDoc OpenAPI** - документация API
- **Micrometer/Prometheus** - метрики

## Разработка

```bash
# Запуск в dev режиме
./gradlew bootRun

# Запуск тестов
./gradlew test

# Проверка кода
./gradlew detekt spotlessCheck
```

## Лицензия

Проект разработан для внутреннего использования.

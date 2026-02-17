-- Удалить state миграции (для повторного PREPARE)
DELETE FROM migration_state WHERE name = '{name}'

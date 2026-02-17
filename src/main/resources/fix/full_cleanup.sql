-- Полный откат миграции (удаляет ВСЕ артефакты миграции)
DROP TRIGGER IF EXISTS {trigger_name} ON {table_name};
DROP FUNCTION IF EXISTS {function_name}();
DROP TABLE IF EXISTS {shadow_table_name};
DELETE FROM migration_state WHERE name = '{name}';
DELETE FROM migration_status WHERE name = '{name}';
DELETE FROM migration_log WHERE name = '{name}'

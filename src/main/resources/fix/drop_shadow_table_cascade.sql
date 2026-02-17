-- Удалить shadow таблицу с зависимостями (ОПАСНО! Сначала проверьте check_fk_dependencies)
DROP TABLE IF EXISTS {shadow_table_name} CASCADE

-- Проверить существует ли shadow таблица
SELECT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = '{shadow_table_name}') as exists

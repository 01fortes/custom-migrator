-- Удалить PK constraint на shadow таблице
ALTER TABLE {shadow_table_name} DROP CONSTRAINT IF EXISTS {pk_name}

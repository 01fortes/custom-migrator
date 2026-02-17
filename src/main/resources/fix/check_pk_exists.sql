-- Проверить существует ли PK constraint
SELECT conname as pk_name
FROM pg_constraint 
WHERE conrelid = '{table_name}'::regclass 
  AND contype = 'p'

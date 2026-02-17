-- Список индексов на таблице
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = '{table_name}'

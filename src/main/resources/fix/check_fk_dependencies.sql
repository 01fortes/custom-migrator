-- Проверить FK зависимости на таблицу (кто ссылается НА эту таблицу)
SELECT 
    tc.table_name as referencing_table, 
    kcu.column_name as referencing_column,
    ccu.table_name as referenced_table
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
  AND ccu.table_name = '{table_name}'

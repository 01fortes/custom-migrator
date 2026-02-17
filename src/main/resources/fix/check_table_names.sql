-- Проверить текущие имена таблиц (полезно после неудачного SWAP)
SELECT tablename 
FROM pg_tables 
WHERE tablename IN ('{table_name}', '{shadow_table_name}', '{shadow_table_name}_temp')
ORDER BY tablename

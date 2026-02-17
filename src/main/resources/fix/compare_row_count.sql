-- Сравнить количество строк между оригинальной и shadow таблицей
SELECT 
    (SELECT count(*) FROM {table_name} WHERE created_at < '{target_date}') as original_count,
    (SELECT count(*) FROM {shadow_table_name}) as shadow_count

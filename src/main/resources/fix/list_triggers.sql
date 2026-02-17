-- Список триггеров на таблице
SELECT tgname as trigger_name 
FROM pg_trigger 
WHERE tgrelid = '{table_name}'::regclass 
  AND NOT tgisinternal

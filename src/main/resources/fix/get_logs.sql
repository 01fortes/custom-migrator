-- Получить последние логи миграции
SELECT * FROM migration_log 
WHERE name = '{name}' 
ORDER BY created_at DESC 
LIMIT 50

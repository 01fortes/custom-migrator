-- Сбросить статус миграции на указанный stage
UPDATE migration_status 
SET stage = '{stage}', 
    status = 'PENDING',
    updated_at = now()
WHERE name = '{name}'

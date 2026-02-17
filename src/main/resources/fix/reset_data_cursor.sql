-- Сбросить курсор миграции данных на начало
UPDATE migration_state 
SET window_start_at = from_start_at,
    window_end_at = from_start_at + INTERVAL '{window_step}',
    updated_at = now()
WHERE name = '{name}'

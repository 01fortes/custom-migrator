-- Скопировать данные за конкретный период вручную
INSERT INTO {shadow_table_name}
SELECT * FROM {table_name}
WHERE created_at >= '{start_date}' AND created_at < '{end_date}'
ON CONFLICT ({pk_columns}) DO NOTHING

WITH st AS (
    SELECT
        from_start_at,
        target_start_at,
        window_start_at,
        window_end_at
    FROM migration_state
    WHERE name = '{migration_name}'
),
win AS (
    SELECT
        st.from_start_at,
        st.target_start_at,

        LEAST(
            COALESCE(st.window_start_at, st.from_start_at),
            st.target_start_at
        ) AS ws,

        LEAST(
            COALESCE(
                st.window_end_at,
                LEAST(COALESCE(st.window_start_at, st.from_start_at), st.target_start_at) + INTERVAL '{window_step}'
            ),
            st.target_start_at
        ) AS we
    FROM st
),
batch AS (
    SELECT t.*
        FROM {table_name} t, win
    WHERE t.created_at >= win.ws
      AND t.created_at <  win.we
),
ins AS (
    INSERT INTO {shadow_table_name}
    SELECT * FROM batch
    ON CONFLICT ({primary_key}) DO NOTHING
    RETURNING 1
),
upd AS (
    UPDATE migration_state s
    SET window_start_at = (SELECT we FROM win),
        window_end_at   = LEAST((SELECT we FROM win) + INTERVAL '{window_step}', (SELECT target_start_at FROM win)),
        updated_at      = now()
    WHERE s.name = '{migration_name}'
    RETURNING
        s.from_start_at,
        s.target_start_at,
        s.window_start_at,
        s.window_end_at
)
SELECT
    (SELECT count(*) FROM batch) AS selected_rows,
    (SELECT count(*) FROM ins)   AS inserted_rows,
    (SELECT window_start_at >= target_start_at FROM upd) AS migration_complete,
    (SELECT window_start_at <  target_start_at FROM upd) AS has_more;
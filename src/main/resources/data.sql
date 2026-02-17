CREATE TABLE IF NOT EXISTS migration_state
(
    name              text        PRIMARY KEY,
    from_start_at     TIMESTAMP   NOT NULL,
    target_start_at   TIMESTAMP   NOT NULL,
    window_start_at   TIMESTAMP   NULL,
    window_end_at     TIMESTAMP   NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS migration_log
(
    name        TEXT        NOT NULL,
    stage       TEXT        NOT NULL,
    status      TEXT        NOT NULL,
    message     TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS  migration_status
(
    name        TEXT        PRIMARY KEY,
    stage       TEXT        NOT NULL,
    status      TEXT        NOT NULL,
    message     TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now()
);
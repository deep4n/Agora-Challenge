-- =============================================
--  V2__create_events_table.sql
-- =============================================

CREATE TABLE events (
    id               BIGSERIAL           PRIMARY KEY,
    title            VARCHAR(255)        NOT NULL,
    description      TEXT,
    location         VARCHAR(255)        NOT NULL,
    event_date       TIMESTAMP           NOT NULL,
    available_seats  INTEGER             NOT NULL CHECK (available_seats >= 0),
    ticket_price     NUMERIC(12, 2)      NOT NULL CHECK (ticket_price >= 0),
    is_active        BOOLEAN             NOT NULL DEFAULT TRUE,
    creator_id       BIGINT              NOT NULL REFERENCES users(id),
    created_at       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_creator_id ON events(creator_id);
CREATE INDEX idx_events_event_date ON events(event_date);
CREATE INDEX idx_events_is_active ON events(is_active);
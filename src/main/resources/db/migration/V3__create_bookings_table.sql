-- =============================================
--  V3__create_bookings_table.sql
-- =============================================

CREATE TABLE bookings (
    id               BIGSERIAL           PRIMARY KEY,
    reference_number VARCHAR(20)         NOT NULL UNIQUE,
    user_id          BIGINT              NOT NULL REFERENCES users(id),
    event_id         BIGINT              NOT NULL REFERENCES events(id),
    num_tickets      INTEGER             NOT NULL CHECK (num_tickets > 0),
    total_price      NUMERIC(12, 2)      NOT NULL,
    status           VARCHAR(20)         NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FR14 — prevent double booking
    CONSTRAINT uq_user_event UNIQUE (user_id, event_id),

    -- validasi status
    CONSTRAINT chk_booking_status CHECK (status IN ('ACTIVE', 'CANCELLED'))
);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_event_id ON bookings(event_id);
CREATE INDEX idx_bookings_reference_number ON bookings(reference_number);
CREATE INDEX idx_bookings_status ON bookings(status);
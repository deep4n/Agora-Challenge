-- =============================================
--  V4__add_version_to_events.sql
--  Tambah kolom version untuk optimistic locking
-- =============================================

ALTER TABLE events ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
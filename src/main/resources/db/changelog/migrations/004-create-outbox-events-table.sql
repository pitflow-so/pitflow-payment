--liquibase formatted sql
--changeset pitflow:004-create-outbox-events-table
CREATE TABLE outbox_events
(
    id              UUID PRIMARY KEY,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB        NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    attempts        INTEGER      NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    created_at      TIMESTAMPTZ  NOT NULL,
    processed_at    TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    last_error      TEXT
);
CREATE INDEX idx_outbox_events_status ON outbox_events (status);
CREATE INDEX idx_outbox_events_next_attempt_at ON outbox_events (next_attempt_at);
CREATE INDEX idx_outbox_events_created_at ON outbox_events (created_at);
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events (aggregate_id);
--rollback DROP TABLE IF EXISTS outbox_events;

--liquibase formatted sql
--changeset pitflow:003-create-webhook-events-table
CREATE TABLE webhook_events
(
    id                  UUID PRIMARY KEY,
    event_key           VARCHAR(255) NOT NULL UNIQUE,
    provider            VARCHAR(32)  NOT NULL,
    provider_event_id   VARCHAR(255),
    provider_payment_id VARCHAR(255),
    action              VARCHAR(100),
    payload             JSONB        NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    attempts            INTEGER      NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    received_at         TIMESTAMPTZ  NOT NULL,
    processed_at        TIMESTAMPTZ,
    next_attempt_at     TIMESTAMPTZ,
    last_error          TEXT
);
CREATE INDEX idx_webhook_events_status ON webhook_events (status);
CREATE INDEX idx_webhook_events_next_attempt_at ON webhook_events (next_attempt_at);
CREATE INDEX idx_webhook_events_received_at ON webhook_events (received_at);
--rollback DROP TABLE IF EXISTS webhook_events;

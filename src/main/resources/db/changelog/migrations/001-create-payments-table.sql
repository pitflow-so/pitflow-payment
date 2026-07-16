--liquibase formatted sql
--changeset pitflow:001-create-payments-table
CREATE TABLE payments
(
    id                       UUID PRIMARY KEY,
    service_order_id         UUID           NOT NULL,
    budget_version           BIGINT         NOT NULL,
    external_reference       VARCHAR(255)   NOT NULL UNIQUE,
    idempotency_key          VARCHAR(255)   NOT NULL UNIQUE,
    idempotency_payload_hash VARCHAR(64)    NOT NULL,
    amount                   NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency                 CHAR(3)        NOT NULL CHECK (currency = 'BRL'),
    status                   VARCHAR(32)    NOT NULL,
    provider                 VARCHAR(32)    NOT NULL,
    payer_email              VARCHAR(320)   NOT NULL,
    approved_at              TIMESTAMPTZ NULL,
    created_at               TIMESTAMPTZ    NOT NULL,
    updated_at               TIMESTAMPTZ    NOT NULL,
    version                  BIGINT         NOT NULL,
    CONSTRAINT uk_payments_service_order_budget UNIQUE (service_order_id, budget_version)
);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_service_order ON payments (service_order_id);
CREATE INDEX idx_payments_created_at ON payments (created_at);
CREATE INDEX idx_payments_updated_at ON payments (updated_at);
--rollback DROP TABLE IF EXISTS payments;

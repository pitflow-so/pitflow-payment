--liquibase formatted sql
--changeset pitflow:002-create-payment-attempts-table
CREATE TABLE payment_attempts
(
    id                     UUID PRIMARY KEY,
    payment_id             UUID         NOT NULL REFERENCES payments (id),
    provider_preference_id VARCHAR(255) NOT NULL UNIQUE,
    provider_payment_id    VARCHAR(255),
    checkout_url           TEXT         NOT NULL,
    provider_status        VARCHAR(100),
    provider_status_detail VARCHAR(255),
    expires_at             TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_payment_attempts_payment_id ON payment_attempts (payment_id);
CREATE UNIQUE INDEX uk_payment_attempts_provider_payment_id ON payment_attempts (provider_payment_id) WHERE provider_payment_id IS NOT NULL;
--rollback DROP TABLE IF EXISTS payment_attempts;

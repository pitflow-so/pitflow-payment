--liquibase formatted sql
--changeset pitflow:005-prepare-saga-checkout-flow
ALTER TABLE payments ALTER COLUMN payer_email DROP NOT NULL;

ALTER TABLE outbox_events ADD COLUMN destination VARCHAR(255);
ALTER TABLE outbox_events ADD COLUMN locked_until TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN lock_id UUID;

CREATE INDEX idx_payment_outbox_claim
    ON outbox_events (status, next_attempt_at, created_at);

--rollback DROP INDEX IF EXISTS idx_payment_outbox_claim;
--rollback ALTER TABLE outbox_events DROP COLUMN IF EXISTS lock_id;
--rollback ALTER TABLE outbox_events DROP COLUMN IF EXISTS locked_until;
--rollback ALTER TABLE outbox_events DROP COLUMN IF EXISTS destination;
--rollback ALTER TABLE payments ALTER COLUMN payer_email SET NOT NULL;

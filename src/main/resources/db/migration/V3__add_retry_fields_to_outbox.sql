ALTER TABLE outbox_events
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE outbox_events
    ADD COLUMN next_retry_at TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE outbox_events
    ADD COLUMN last_error TEXT;
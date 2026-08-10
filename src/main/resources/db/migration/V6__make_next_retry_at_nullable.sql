ALTER TABLE outbox_events
    ALTER COLUMN next_retry_at DROP NOT NULL;
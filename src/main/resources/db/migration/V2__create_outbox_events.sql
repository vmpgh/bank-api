CREATE TABLE outbox_events
(
    id UUID PRIMARY KEY,

    event_type VARCHAR(100) NOT NULL,

    payload TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL,

    status VARCHAR(20) NOT NULL
);

CREATE INDEX idx_outbox_status
    ON outbox_events(status);

CREATE INDEX idx_outbox_created_at
    ON outbox_events(created_at);
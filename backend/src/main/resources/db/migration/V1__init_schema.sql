-- Idempotency ledger: inserted first inside the processing transaction.
-- A unique-key conflict == duplicate delivery -> skip (at-least-once delivery, exactly-once effect).
CREATE TABLE processed_event (
    event_id     VARCHAR(64) PRIMARY KEY,
    outcome      VARCHAR(16) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Transactional outbox: webhook acceptance and "to be published" are one atomic DB write.
CREATE TABLE outbox_event (
    id         BIGSERIAL PRIMARY KEY,
    event_id   VARCHAR(64) NOT NULL UNIQUE,
    payload    TEXT NOT NULL,
    status     VARCHAR(8) NOT NULL DEFAULT 'NEW',
    attempts   INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at    TIMESTAMPTZ
);
CREATE INDEX idx_outbox_new ON outbox_event (id) WHERE status = 'NEW';

CREATE TABLE card_transaction (
    event_id      VARCHAR(64) PRIMARY KEY,
    event_type    VARCHAR(16) NOT NULL,
    card_token    VARCHAR(64) NOT NULL,
    merchant_name VARCHAR(120) NOT NULL,
    mcc           CHAR(4) NOT NULL,
    amount        NUMERIC(18,2) NOT NULL,
    currency      CHAR(3) NOT NULL,
    country       CHAR(2) NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL,
    decision      VARCHAR(12) NOT NULL,
    decline_reason VARCHAR(200),
    risk_score    NUMERIC(5,4) NOT NULL,
    lake_batch_id BIGINT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_txn_created ON card_transaction (created_at DESC);
CREATE INDEX idx_txn_decision ON card_transaction (decision);
CREATE INDEX idx_txn_unexported ON card_transaction (created_at) WHERE lake_batch_id IS NULL;

CREATE TABLE dq_quarantine (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(64) NOT NULL,
    raw_payload  TEXT NOT NULL,
    failed_rules TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    replayed     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE dead_letter (
    id          BIGSERIAL PRIMARY KEY,
    event_id    VARCHAR(64) NOT NULL,
    raw_payload TEXT NOT NULL,
    error       VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    replayed    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE lake_batch (
    id           BIGSERIAL PRIMARY KEY,
    object_key   TEXT,
    record_count INT NOT NULL,
    bytes        BIGINT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

# ADR-0001: Transactional outbox + idempotent consumer for "exactly-once" processing
**Status:** Accepted

## Context
Webhooks are delivered at-least-once; RabbitMQ is at-least-once. Dual writes (DB + broker) can lose or duplicate events. True exactly-once across HTTP, DB and AMQP is not achievable.

## Decision
Persist accepted events in an `outbox_event` table inside the request transaction; a relay publishes with publisher confirms using `FOR UPDATE SKIP LOCKED`. The consumer claims `eventId` in a `processed_event` ledger **in the same DB transaction** as its business write. RabbitMQ quorum queues, bounded in-process retries with exponential backoff, then a recoverer persists a `dead_letter` row and rejects to a DLX/DLQ.

## Consequences
+ No acknowledged event lost; duplicates are harmless; horizontally scalable relays/consumers.
+ Failures are inspectable and replayable from SQL and API.
− Extra write per event; relay adds latency (~poll interval); ledger grows (needs TTL/partitioning at scale).
− Ordering is not guaranteed.

## Alternatives
Kafka + transactions (heavier ops, not in the JD stack); CDC/Debezium (more moving parts); "publish then commit" (loses events).

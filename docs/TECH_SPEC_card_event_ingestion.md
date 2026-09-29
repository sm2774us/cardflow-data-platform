# Tech Spec — Card Event Ingestion & Lake Export

| | |
|---|---|
| Status | Accepted (v1) |
| Owners | Platform / Data Integration |
| Related | ADR-0001 … ADR-0004 |

## 1. Problem
Upstream card-processing systems emit authorization events via webhook. Downstream consumers (authorization analytics, fraud, the enterprise data lake, Snowflake) require each event to be processed **effectively once**, validated against data-quality rules, and made queryable within seconds — without losing events during broker, database, or dependency failures.

## 2. Goals / Non-goals
**Goals** — durable acceptance (no acknowledged event is ever lost); effectively-once state change; poison-message isolation; replayable failures; partitioned, columnar-friendly lake output; observable end to end.
**Non-goals** — a true exactly-once *transport* (impossible over HTTP/AMQP; we provide at-least-once delivery + idempotent processing); PCI-scope card-data storage (PANs are rejected by the DQ gate); multi-region active/active.

## 3. Contract (v1)
`POST /api/v1/webhooks/card-events` · `Content-Type: application/json` · `X-Signature: sha256=<hex HMAC-SHA256(rawBody, sharedSecret)>`

| Field | Type | Rule |
|---|---|---|
| eventId | string (UUID recommended) | required, unique; the idempotency key |
| schemaVersion | int | required; `1` |
| eventType | enum | `AUTHORIZATION`, `CLEARING`, `REVERSAL` |
| cardToken | string | required; `tok_*` token — **never a PAN** |
| merchantName | string | required, non-blank |
| mcc | string | 4 digits |
| amount | decimal | within configured range (> 0) |
| currency | string | ISO-4217 |
| country | string | ISO-3166 alpha-2 |
| occurredAt | RFC-3339 instant | required; not >5 min in the future, not >30 days old |

Responses: `202 {eventId, duplicate}` accepted (including redelivery) · `401` bad/missing signature · `400/415` malformed transport · `5xx` retry (sender should back off).
Errors use RFC 7807 `application/problem+json` and echo `X-Correlation-Id`.

## 4. Delivery semantics
```
HTTP 202 ⇐ [ tx: INSERT outbox_event  ON CONFLICT(event_id) DO NOTHING ]      (durable acceptance)
outbox relay: SELECT … FOR UPDATE SKIP LOCKED → publish w/ confirms → mark sent (at-least-once to broker)
consumer:     [ tx: INSERT processed_event(event_id) ON CONFLICT DO NOTHING ]
              rows==0 → duplicate → ack, no side effects
              rows==1 → DQ gate → authorize → INSERT card_transaction    (same tx ⇒ atomic with ledger claim)
```
* **Acceptance is atomic with the outbox row**, so a crash between "HTTP 202" and "publish" cannot lose an event.
* **Publisher confirms** + `SKIP LOCKED` let N relay replicas run without double-claiming; worst case is a duplicate publish, absorbed by the consumer ledger.
* **Effectively-once** = at-least-once delivery × idempotent consumer keyed by `eventId`, with the ledger claim and the business write in a single DB transaction (a rollback un-claims the event so retry can succeed).
* **Ordering** is not guaranteed and not required; `occurredAt` is authoritative for analytics.

## 5. Failure modes
| Failure | Behaviour | Recovery |
|---|---|---|
| Duplicate webhook delivery | Unique key on outbox; `duplicate=true` | none needed |
| Broker down | Events accumulate in outbox; relay retries; API stays available | automatic on broker return |
| Relay crash mid-batch | Uncommitted rows re-claimed | automatic |
| Consumer exception (transient) | In-process exponential backoff (500 ms ×2, cap 5 s, 4 attempts) | automatic |
| Poison message | After retries: `dead_letter` row written, message rejected to DLX → DLQ | fix, then `POST /api/v1/dlq/{id}/replay` |
| DQ rule violation | Business-level reject → `dq_quarantine` (not retried, not lost) | fix upstream / rule, then `POST /api/v1/dq/quarantine/{id}/replay` |
| Fraud scorer slow/failing | Resilience4j circuit breaker → deterministic rules-only fallback, decision flagged `degraded=true` | automatic half-open probe |
| DB failover | Requests fail fast (5xx) → sender retries; consumers redeliver | automatic |
| Lake sink (S3) failure | Batch not marked exported; retried next tick | automatic |
| Spoofed/tampered webhook | HMAC mismatch → 401 (constant-time compare) | rotate secret |

## 6. Data-quality gate
Ten rules (see `GET /api/v1/dq/rules`): `REQUIRED_FIELDS`, `SCHEMA_VERSION`, `EVENT_TYPE_KNOWN`, `AMOUNT_RANGE`, `CURRENCY_ISO4217`, `COUNTRY_ISO3166`, `MCC_FORMAT`, `TOKENIZED_CARD`, `NO_PAN_LEAK` (PAN-shaped digit runs in any field), `TIMESTAMP_SANE`. Promotion criterion for the pipeline: *quarantine rate and DLQ depth within SLO*, not only green unit tests.

## 7. Lake export
`LakeExportJob` claims un-exported rows (`SKIP LOCKED`), writes **NDJSON.gz** to `s3://<lake>/card_transactions/dt=YYYY-MM-DD/hour=HH/part-<uuid>.json.gz`, records a `lake_batch` row. Glue table uses **partition projection** (no crawlers); Athena workgroup enforces scan limits; Snowflake ingests via external stage + Snowpipe/Streams/Tasks (`data/snowflake/`).

## 8. SLOs (targets for a production deployment)
| SLI | Objective |
|---|---|
| Webhook availability | 99.9 % monthly |
| Accept latency p99 | < 150 ms |
| Accept → transaction visible p99 | < 5 s |
| Accepted-event loss | 0 |
| DLQ depth | 0 sustained > 15 min pages on-call |
| Quarantine rate | < 1 % of volume (ticket above) |

## 9. Observability
Prometheus via Actuator; correlation-id propagated HTTP → outbox → AMQP header → logs; circuit-breaker state exposed on `/actuator/health`. Suggested alerts: outbox age > 60 s, DLQ depth > 0, quarantine-rate spike, breaker open > 5 min, lake batch lag > 3 intervals.

## 10. Runbook (abridged)
1. **DLQ non-empty** → `GET /api/v1/dlq`; read `error`; fix cause; replay one, verify, then replay rest.
2. **Quarantine spike** → `GET /api/v1/dq/quarantine`; group by `failedRules`; contact producer or adjust rule with an ADR.
3. **Outbox growing** → check broker health/credentials/queue limits; relay logs; scale relay replicas.
4. **Lake lag** → check IAM/KMS on the lake bucket and `lake_batch` rows.
5. **Secret rotation** → deploy new secret alongside old (dual-verify window), then remove old.

## 11. Security
HMAC authenticity on ingest; non-root containers with read-only root FS; secrets from env/Secrets Manager (never committed); KMS-encrypted, versioned, public-access-blocked S3; least-privilege IAM for Snowflake; PAN-leak guard. Known gap: the read/API endpoints are unauthenticated in this showcase (add OIDC/JWT via Spring Security before real use).

## 12. Open questions
Schema-registry adoption for v2; per-tenant secrets; exactly-once into Snowflake via Snowpipe Streaming vs. file-based loads.

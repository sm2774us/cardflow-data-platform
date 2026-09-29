# CardFlow — Event-Driven Card Data Platform

[![CI](https://github.com/sm2774us/cardflow-data-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/sm2774us/cardflow-data-platform/actions/workflows/ci.yml)

A production-shaped reference implementation of a **card-event ingestion → validation → real-time authorization → data-lake** pipeline: Spring Boot 3 (Java 17) · React 18 + TypeScript 5.6 · RabbitMQ · PostgreSQL · AWS (S3/Glue/Athena, EKS, Amazon MQ) via **OpenTofu** (open-source, Terraform-compatible) · Kubernetes/OpenShift via Kustomize · Snowflake SQL · GitHub Actions.

## Architecture
```
 Upstream ──HMAC webhook──▶ API ──tx──▶ outbox_event (Postgres)
                                           │  relay: SKIP LOCKED + publisher confirms
                                           ▼
                              RabbitMQ quorum queue ──▶ retry (exp. backoff) ──▶ DLX/DLQ + dead_letter row
                                           ▼
                     idempotent consumer  (processed_event ledger, same tx)
                                           ▼
                        Data-Quality Gate (10 rules) ── fail ──▶ dq_quarantine (replayable)
                                           ▼
                Authorization engine: rules + velocity + fraud score (circuit breaker + fallback)
                                           ▼
                                   card_transaction ──▶ SSE live stream ──▶ React console
                                           ▼
                     LakeExportJob → S3 NDJSON.gz  dt=/hour= ──▶ Glue/Athena · Snowflake (Streams/Tasks)
```
Design detail: [`docs/TECH_SPEC_card_event_ingestion.md`](docs/TECH_SPEC_card_event_ingestion.md) · Decisions: [`docs/adr/`](docs/adr).

## Quickstart
```bash
make up                      # docker compose: Postgres, RabbitMQ, LocalStack(S3), backend, frontend
open http://localhost:8081   # console  (API :8080 · Swagger /swagger-ui.html · RabbitMQ UI :15672 guest/guest)
./scripts/send-webhook.sh    # send one signed event
```
In the console, open **Live** and press *Simulate traffic* to inject valid, duplicate, invalid, and (with chaos enabled) poison events.

| Command | What it runs |
|---|---|
| `make backend-test` | JUnit unit tests (no Docker) |
| `make backend-verify` | + Testcontainers/RestAssured integration tests (Docker) |
| `make frontend-all` | npm ci, ESLint, `tsc`, Vitest, Vite build |
| `make api-tests` | Karate + Newman against a running stack |
| `make tofu-validate` / `make k8s-render` | OpenTofu fmt-check/validate · Kustomize dev/prod/openshift |

## Repository workflow (deliberately simple, always human-supervised)
* `main` is protected by a ruleset (`.github/rulesets/protect-main.json`): no direct pushes, no force-push, no deletion, and CI (`backend`, `frontend`, `infra`) must pass before a pull request can merge.
* **No bots open branches or PRs.** No auto-merge, no Dependabot version PRs, no AI-generated PRs. You create one short-lived branch, open one PR, read the diff, and merge it yourself; the branch is auto-deleted.
* Three workflows only: `ci` (push to `main` + PRs), `api-tests` (manual), `release` (runs only when you push a `vX.Y.Z` tag).
* Step-by-step setup for beginners: **[WALKTHROUGH.md](WALKTHROUGH.md)**.

## Job-requirement traceability
The same matrix is rendered in-app on the **Architecture** page (`frontend/src/data/jdMatrix.ts`).

| Requirement | Where |
|---|---|
| Event-driven ingestion (RabbitMQ, webhooks) with retry + idempotency | `ingest/`, `processing/`, `config/AmqpConfig` |
| Exactly-once *effect* | outbox + `processed_event` ledger (ADR-0001) |
| Data pipelines / data lake on AWS (S3, Glue, Athena) | `lake/`, `infra/opentofu/modules/data_lake` |
| Snowflake (Streams, Tasks, Snowpark, sharing) | `data/snowflake/` |
| Data-quality gates as promotion criteria | `processing/DataQualityGate`, ADR-0002, PR template |
| RestAssured · Karate · Postman/Newman | `PipelineIT`, `api-tests/karate`, `api-tests/postman` |
| Java / Spring Boot services & APIs | `backend/` |
| Real-time authorization | `authorization/` |
| Kubernetes / OpenShift | `k8s/` (restricted-SCC friendly, Route overlay) |
| AWS infrastructure as code (Terraform-compatible HCL) | `infra/opentofu`, ADR-0005 |
| Technical specification writing | `docs/` |
| AI tooling with production guardrails | `FraudScorer` port, `AGENTS.md` |
| Payments / fintech domain | whole project |

## Frontend stack
React 18.3 · TypeScript 5.6 · Vite · Tailwind · Radix primitives + shadcn-style owned components · TanStack Query / Table / Router (typed search params) · Zustand · Motion · Recharts · en/es/pt UI.

## Honest limitations
* Only ingest is authenticated (HMAC). Read/admin APIs have **no authN/Z** — add Spring Security + OIDC before real use.
* Velocity counters and the SSE bus are per-replica (in-memory). Use Redis / a broker fan-out to scale them.
* `LogisticFraudScorer` is an in-memory stand-in, not a trained model.
* Snowflake SQL and the OpenTofu code were written to be reviewed and adapted; they were not applied to real accounts.
* es/pt UI strings are draft-quality and need native-speaker review.
* Before publishing: run `scripts/set-owner` with your GitHub username (see [WALKTHROUGH.md](WALKTHROUGH.md)), set the state bucket names in `infra/opentofu/envs/*`, and replace all secrets.

## License
MIT

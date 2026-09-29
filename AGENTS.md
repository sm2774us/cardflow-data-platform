# AGENTS.md — guardrails for AI coding assistants (Claude, Copilot, Cursor, …)

AI tools are welcome here; unreviewed AI output is not. The human author owns every line.

## Project rules
* Backend: Java 17, Spring Boot 3, **Spring JDBC (no JPA)**, Flyway forward-only migrations, RFC 7807 errors, constructor injection.
* Keep consumers **idempotent**; never introduce a second write path that bypasses the outbox or the `processed_event` ledger.
* Never log, store, or put in fixtures: PANs, CVVs, secrets. Card data is tokenized (`tok_*`); the DQ gate rejects PAN-like values.
* Every API change ⇒ update `api-tests/karate` features **and** the Postman collection **and** the tech spec if semantics change.
* Every new DQ rule ⇒ unit test (pass + fail case) and an ADR note if it can quarantine previously-accepted data.
* Frontend: React 18 / TS strict, TanStack Query for server state, Zustand only for UI state, components in `src/components/ui` are owned code.
* Infra: `tofu fmt` (OpenTofu), no wildcard IAM, no public buckets, no secrets in tfvars or manifests.

## Working agreement for AI-assisted changes
1. State the intent and constraints in the prompt; ask for a plan before code on non-trivial work.
2. Run `make backend-test frontend-all` (and `backend-verify` when touching ingestion) before opening a PR.
3. Verify claims: do not accept invented APIs, versions, or config keys — check the docs or compile it.
4. Ticked "AI-assisted" box in the PR template means: you reviewed and can explain every line.
5. Prefer small diffs; refuse drive-by refactors.

## Where AI is used in the product
`authorization/FraudScorer` is a port. The default `LogisticFraudScorer` is an in-memory stand-in for a model; swap in a remote model endpoint (SageMaker/Bedrock/…) behind the same interface. `ResilientFraudScorer` supplies timeout, circuit breaker, and a deterministic rules-only fallback so a model outage degrades decisions instead of stopping authorization.

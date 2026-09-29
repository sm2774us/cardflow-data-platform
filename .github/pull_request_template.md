## What & why
<!-- One paragraph. Link the issue / ADR / tech-spec section. -->

## Change type
- [ ] Feature  - [ ] Fix  - [ ] Refactor  - [ ] Infra  - [ ] Docs

## Quality gates
- [ ] Unit + integration tests added/updated (`make backend-verify`, `make frontend-all`)
- [ ] API contract changes reflected in Karate features and the Postman collection
- [ ] **Data-quality impact assessed** (new/changed DQ rules, quarantine behaviour, lake schema/partitioning)
- [ ] Idempotency & retry behaviour considered (duplicate delivery, poison messages, replay)
- [ ] No PAN/PII in logs, fixtures, or test data
- [ ] Migrations are forward-only and backwards compatible with the previous release
- [ ] `tofu fmt` / `kustomize build` clean for touched infra

## AI-assisted changes (see AGENTS.md)
- [ ] N/A — no AI assistance
- [ ] AI-assisted; I reviewed every line, ran the tests, and can explain the change

## Rollout / rollback
<!-- Flags, migrations, backfill, how to revert. -->

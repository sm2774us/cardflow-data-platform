# ADR-0002: Data-quality gate with quarantine (not retry, not drop)
**Status:** Accepted

## Context
Bad data is deterministic: retrying never fixes it, dropping it hides it, and letting it through pollutes the lake and models. The JD explicitly calls for promotion criteria beyond code quality.

## Decision
A rule-based `DataQualityGate` runs before any business logic. Failures are written to `dq_quarantine` with the list of failed rules and the raw payload, acked (no retry storm), surfaced in the UI/API and replayable after a fix. Rules are unit-tested individually; the PAN-leak guard is mandatory. Release criteria include quarantine-rate and DLQ-depth SLOs.

## Consequences
+ Clean downstream data; producers get concrete feedback; incident triage is fast.
− Rules are code: changes need review and (for breaking ones) a schema-version bump.
− A wrong rule can quarantine good data — mitigated by replay.

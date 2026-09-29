# ADR-0004: Lake export format and partitioning
**Status:** Accepted

## Decision
Micro-batches are written as gzip-compressed NDJSON under `dt=YYYY-MM-DD/hour=HH/`. Glue table uses partition projection (no crawlers, no `MSCK REPAIR`). Athena workgroup has a per-query scan cutoff. Snowflake loads from an external stage; Streams + Tasks materialise curated tables.

## Rationale
NDJSON is trivially debuggable and schema-evolution friendly for a first iteration; date/hour partitions match the dominant query pattern and keep scans cheap.

## Consequences / next step
Small files at low volume — compact and convert to Parquet (Glue job or Snowflake `COPY INTO … PARQUET`) once volume justifies it; introduce a schema registry for v2.

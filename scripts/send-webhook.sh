#!/usr/bin/env bash
# Sends one HMAC-signed card event to the local webhook. Usage: ./scripts/send-webhook.sh [eventId]
set -euo pipefail
SECRET="${WEBHOOK_SECRET:-dev-only-change-me}"
ID="${1:-$(uuidgen)}"
BODY=$(printf '{"eventId":"%s","schemaVersion":1,"eventType":"AUTHORIZATION","cardToken":"tok_DEMO00000001","merchantName":"Cafe Aroma","mcc":"5814","amount":12.5,"currency":"USD","country":"US","occurredAt":"%s"}' "$ID" "$(date -u +%Y-%m-%dT%H:%M:%SZ)")
SIG="sha256=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -hex | sed 's/^.* //')"
curl -sS -X POST "${API:-http://localhost:8080}/api/v1/webhooks/card-events" -H 'Content-Type: application/json' -H "X-Signature: $SIG" -d "$BODY"; echo

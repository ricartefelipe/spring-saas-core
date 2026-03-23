#!/usr/bin/env bash
# E2E HTTP: convite de utilizador novo (POST /v1/users/invite).
# Pré-requisito: stack a correr (ex.: ./scripts/up.sh ou mvn spring-boot:run com Postgres/Redis/RabbitMQ).
#
# Uso:
#   ./scripts/e2e-invite-user.sh
#   BASE_URL=http://localhost:8080 ./scripts/e2e-invite-user.sh

set -euo pipefail
cd "$(dirname "$0")/.."

BASE="${BASE_URL:-http://localhost:8080}"
# Tenant demo do seed (009) — mesmo usado em smoke.sh
TENANT_ID="${E2E_TENANT_ID:-00000000-0000-0000-0000-000000000002}"
UNIQ="${E2E_EMAIL_UNIQ:-$(date +%s)}"
NEW_EMAIL="${E2E_NEW_EMAIL:-novo.${UNIQ}@e2e-invite.test.local}"

echo "=== E2E: convite de utilizador ==="
echo "  BASE=$BASE"
echo "  TENANT_ID=$TENANT_ID"
echo ""

if ! curl -sf "$BASE/actuator/health/liveness" >/dev/null; then
  echo "ERRO: API não responde em $BASE (subir com ./scripts/up.sh ou spring-boot:run)."
  exit 1
fi

echo "--- 1. POST /v1/dev/token (admin com users:write) ---"
TOKEN_RESP=$(curl -sf -X POST "$BASE/v1/dev/token" \
  -H "Content-Type: application/json" \
  -d "{
    \"sub\": \"e2e-admin-invite@test.local\",
    \"tid\": \"$TENANT_ID\",
    \"roles\": [\"admin\"],
    \"perms\": [\"users:read\", \"users:write\", \"admin:write\"],
    \"plan\": \"pro\",
    \"region\": \"us-east-1\"
  }")
TOKEN=$(echo "$TOKEN_RESP" | jq -r '.access_token')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "Falha ao obter token: $TOKEN_RESP"
  exit 1
fi
echo "  Token OK (len=${#TOKEN})"

echo ""
echo "--- 2. POST /v1/users/invite ---"
BODY=$(jq -n \
  --arg name "Utilizador E2E" \
  --arg email "$NEW_EMAIL" \
  '{name: $name, email: $email, roles: ["member"]}')

INVITE_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/v1/users/invite" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: e2e-invite-user" \
  -d "$BODY")

HTTP=$(echo "$INVITE_RESP" | tail -n1)
JSON=$(echo "$INVITE_RESP" | sed '$d')

echo "  HTTP $HTTP"
echo "$JSON" | jq . 2>/dev/null || echo "$JSON"

if [ "$HTTP" != "201" ]; then
  echo ""
  echo "FALHA: esperado 201, obtido $HTTP"
  exit 1
fi

EMAIL_OUT=$(echo "$JSON" | jq -r '.email // empty')
if [ "$EMAIL_OUT" != "$NEW_EMAIL" ]; then
  echo "AVISO: email na resposta não coincide (got=$EMAIL_OUT)"
fi

echo ""
echo "=== OK: utilizador convidado (201) ==="
exit 0

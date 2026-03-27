#!/usr/bin/env bash
set -euo pipefail

# Smoke HTTP pos-deploy (staging/producao). Nao executa testes unitarios — use CI principal.
#
# Variaveis:
#   CORE_SMOKE_URL ou SMOKE_BASE_URL — URL base publica (ex.: https://core.example.railway.app)
#   SMOKE_REQUIRE_URL=1 — falha se URL nao estiver definida (pipeline que exige staging)

BASE_URL="${CORE_SMOKE_URL:-${SMOKE_BASE_URL:-}}"
REQUIRE="${SMOKE_REQUIRE_URL:-0}"

if [[ -z "$BASE_URL" ]]; then
  echo "[smoke] CORE_SMOKE_URL/SMOKE_BASE_URL nao definido — smoke HTTP ignorado."
  if [[ "$REQUIRE" == "1" ]]; then
    echo "[smoke] SMOKE_REQUIRE_URL=1 exige URL."
    exit 1
  fi
  exit 0
fi

BASE_URL="${BASE_URL%/}"
echo "[smoke] spring-saas-core — base ${BASE_URL}"

curl -sfS --max-time 20 "$BASE_URL/healthz" >/dev/null || {
  echo "[smoke] FALHA: GET /healthz"
  exit 1
}
echo "[smoke] OK /healthz"

if curl -sfS --max-time 15 -o /dev/null "$BASE_URL/actuator/health/liveness" 2>/dev/null; then
  echo "[smoke] OK /actuator/health/liveness"
else
  echo "[smoke] aviso: /actuator/health/liveness nao respondeu (opcional)"
fi

BODY=$(curl -sfS --max-time 20 "$BASE_URL/v3/api-docs") || {
  echo "[smoke] FALHA: GET /v3/api-docs"
  exit 1
}
if ! grep -q openapi <<<"$BODY" && ! grep -q swagger <<<"$BODY"; then
  echo "[smoke] FALHA: /v3/api-docs nao parece OpenAPI"
  exit 1
fi
echo "[smoke] OK /v3/api-docs (contrato basico)"
echo "[smoke] concluido com sucesso"

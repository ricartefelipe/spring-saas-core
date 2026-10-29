#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."

# A base pode ser apontada para outra porta quando a 8080 do host ja estiver tomada.
BASE="${SMOKE_BASE_URL:-http://localhost:8080}"
BASE="${BASE%/}"
PASS=0
FAIL=0

# Espera a aplicacao responder, com prazo e diagnostico — e confere que quem respondeu e ela.
#
# Antes, o roteiro comecava a medir no instante zero contra a porta 8080 fixa. Se a aplicacao ainda
# estivesse subindo, ou se a porta pertencesse a outro servico da maquina, os erros apareciam como
# codigos HTTP inexplicados (500, 502) atribuidos a esta aplicacao.
wait_for_app() {
  local deadline=$((SECONDS + ${SMOKE_WAIT_SECONDS:-120}))
  local code body
  while [ "$SECONDS" -lt "$deadline" ]; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$BASE/actuator/health/liveness" || echo 000)
    if [ "$code" = "200" ]; then
      body=$(curl -s --max-time 10 "$BASE/v3/api-docs" || true)
      # O titulo do contrato identifica a aplicacao: sem esta conferencia, outro servico publicado
      # na mesma porta era medido como se fosse esta e os erros apareciam atribuidos a ela.
      if printf '%s' "$body" | grep -q "Spring SaaS Core"; then
        echo "Aplicacao pronta em $BASE"
        return 0
      fi
      echo "ERRO: $BASE respondeu, mas /v3/api-docs nao declara a Spring SaaS Core API." >&2
      echo "      Outro servico esta publicado nessa porta. Use SMOKE_BASE_URL para apontar a certa." >&2
      return 1
    fi
    sleep 3
  done
  echo "ERRO: $BASE nao respondeu 200 em /actuator/health/liveness dentro do prazo." >&2
  echo "      Ultimo codigo observado: ${code}." >&2
  if command -v docker >/dev/null 2>&1; then
    echo "      Estado dos conteineres e ultimas linhas do app:" >&2
    docker compose ps >&2 2>/dev/null || true
    docker compose logs --tail 30 app >&2 2>/dev/null || true
  fi
  return 1
}

if ! wait_for_app; then
  echo ""
  echo "=== Smoke abortado: a aplicacao nao esta atendendo em $BASE ==="
  exit 1
fi

check() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    echo "  PASS: $desc (got $actual)"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: $desc (expected $expected, got $actual)"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Smoke Test Suite ==="
echo ""

echo "--- 1. Health endpoints ---"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/healthz")
check "GET /healthz" "200" "$HTTP"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/readyz")
check "GET /readyz" "200" "$HTTP"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/health/liveness")
check "GET /actuator/health/liveness" "200" "$HTTP"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/health/readiness")
check "GET /actuator/health/readiness" "200" "$HTTP"

echo ""
echo "--- 2. OpenAPI ---"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v3/api-docs")
check "GET /v3/api-docs" "200" "$HTTP"

echo ""
echo "--- 3. Prometheus metrics ---"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/prometheus")
check "GET /actuator/prometheus" "200" "$HTTP"

echo ""
echo "--- 4. Dev token ---"
TOKEN_RESP=$(curl -sf -X POST "$BASE/v1/dev/token" \
  -H "Content-Type: application/json" \
  -d '{
    "sub": "smoke-admin@test.local",
    "tid": "00000000-0000-0000-0000-000000000001",
    "roles": ["admin"],
    "perms": ["tenants:read","tenants:write","policies:read","policies:write","flags:read","flags:write","audit:read"],
    "plan": "enterprise",
    "region": "global"
  }')
TOKEN=$(echo "$TOKEN_RESP" | jq -r '.access_token')
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  echo "  PASS: POST /v1/dev/token returned token"
  PASS=$((PASS + 1))
else
  echo "  FAIL: POST /v1/dev/token"
  FAIL=$((FAIL + 1))
fi

echo ""
echo "--- 5. GET /v1/me ---"
ME=$(curl -sf "$BASE/v1/me" -H "Authorization: Bearer $TOKEN" -H "X-Correlation-Id: smoke-test-1")
ME_SUB=$(echo "$ME" | jq -r '.sub')
check "GET /v1/me sub" "smoke-admin@test.local" "$ME_SUB"
ME_CORR=$(echo "$ME" | jq -r '.correlation_id')
check "GET /v1/me correlation_id" "smoke-test-1" "$ME_CORR"

echo ""
echo "--- 6. Correlation ID header ---"
RESP_CORR=$(curl -sI "$BASE/v1/me" -H "Authorization: Bearer $TOKEN" | grep -i "x-correlation-id" | tr -d '\r' | awk '{print $2}')
if [ -n "$RESP_CORR" ]; then
  echo "  PASS: X-Correlation-Id returned in response header"
  PASS=$((PASS + 1))
else
  echo "  FAIL: X-Correlation-Id not in response header"
  FAIL=$((FAIL + 1))
fi

echo ""
echo "--- 7. Tenants CRUD ---"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/tenants" -H "Authorization: Bearer $TOKEN")
check "GET /v1/tenants" "200" "$HTTP"

CREATE_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/v1/tenants" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke Corp","plan":"pro","region":"us-west-2"}')
CREATE_HTTP=$(echo "$CREATE_RESP" | tail -1)
CREATE_BODY=$(echo "$CREATE_RESP" | sed '$d')
check "POST /v1/tenants" "201" "$CREATE_HTTP"
SMOKE_TID=$(echo "$CREATE_BODY" | jq -r '.id // empty')

if [ -n "$SMOKE_TID" ]; then
  HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/tenants/$SMOKE_TID" -H "Authorization: Bearer $TOKEN")
  check "GET /v1/tenants/{id}" "200" "$HTTP"

  HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH "$BASE/v1/tenants/$SMOKE_TID" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d '{"name":"Smoke Corp Updated"}')
  check "PATCH /v1/tenants/{id}" "200" "$HTTP"

  HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/v1/tenants/$SMOKE_TID" \
    -H "Authorization: Bearer $TOKEN")
  check "DELETE /v1/tenants/{id}" "204" "$HTTP"
fi

echo ""
echo "--- 8. Policies CRUD ---"
CREATE_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/v1/policies" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"permissionCode":"smoke:test","effect":"ALLOW","allowedPlans":["pro"],"allowedRegions":[],"enabled":true,"notes":"smoke test"}')
CREATE_HTTP=$(echo "$CREATE_RESP" | tail -1)
CREATE_BODY=$(echo "$CREATE_RESP" | sed '$d')
check "POST /v1/policies" "201" "$CREATE_HTTP"
SMOKE_PID=$(echo "$CREATE_BODY" | jq -r '.id // empty')

if [ -n "$SMOKE_PID" ]; then
  HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/policies/$SMOKE_PID" -H "Authorization: Bearer $TOKEN")
  check "GET /v1/policies/{id}" "200" "$HTTP"
  HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/v1/policies/$SMOKE_PID" -H "Authorization: Bearer $TOKEN")
  check "DELETE /v1/policies/{id}" "204" "$HTTP"
fi

echo ""
echo "--- 9. Feature Flags CRUD ---"
DEMO_TID="00000000-0000-0000-0000-000000000002"
DEMO_TOKEN=$(curl -sf -X POST "$BASE/v1/dev/token" -H "Content-Type: application/json" \
  -d "{\"sub\":\"demo@test.local\",\"tid\":\"$DEMO_TID\",\"roles\":[\"admin\"],\"perms\":[\"flags:read\",\"flags:write\"],\"plan\":\"pro\",\"region\":\"us-east-1\"}" | jq -r '.access_token')

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/v1/tenants/$DEMO_TID/flags" \
  -H "Authorization: Bearer $DEMO_TOKEN" -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $DEMO_TID" \
  -d '{"name":"smoke_flag","enabled":true,"rolloutPercent":50,"allowedRoles":["admin"]}')
check "POST /v1/tenants/{id}/flags" "201" "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/tenants/$DEMO_TID/flags" \
  -H "Authorization: Bearer $DEMO_TOKEN" -H "X-Tenant-Id: $DEMO_TID")
check "GET /v1/tenants/{id}/flags" "200" "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH "$BASE/v1/tenants/$DEMO_TID/flags/smoke_flag" \
  -H "Authorization: Bearer $DEMO_TOKEN" -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $DEMO_TID" \
  -d '{"enabled":false}')
check "PATCH /v1/tenants/{id}/flags/{name}" "200" "$HTTP"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/v1/tenants/$DEMO_TID/flags/smoke_flag" \
  -H "Authorization: Bearer $DEMO_TOKEN" -H "X-Tenant-Id: $DEMO_TID")
check "DELETE /v1/tenants/{id}/flags/{name}" "204" "$HTTP"

echo ""
echo "--- 10. Audit Log ---"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/audit" -H "Authorization: Bearer $TOKEN")
check "GET /v1/audit" "200" "$HTTP"
AUDIT_COUNT=$(curl -sf "$BASE/v1/audit" -H "Authorization: Bearer $TOKEN" | jq '.totalElements')
if [ "$AUDIT_COUNT" -gt 0 ] 2>/dev/null; then
  echo "  PASS: Audit log has $AUDIT_COUNT entries"
  PASS=$((PASS + 1))
else
  echo "  FAIL: Audit log empty"
  FAIL=$((FAIL + 1))
fi
FROM="2000-01-01T00:00:00Z"
TO="2030-12-31T23:59:59Z"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/audit/export?from=$FROM&to=$TO&format=json" -H "Authorization: Bearer $TOKEN")
check "GET /v1/audit/export (compliance)" "200" "$HTTP"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/audit/export?from=$FROM&to=$TO&format=csv" -H "Authorization: Bearer $TOKEN")
check "GET /v1/audit/export format=csv" "200" "$HTTP"

echo ""
echo "--- 11. Tenant Snapshot endpoints ---"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/tenants/$DEMO_TID/snapshot" -H "Authorization: Bearer $TOKEN")
check "GET /v1/tenants/{id}/snapshot" "200" "$HTTP"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/tenants/$DEMO_TID/snapshot?include=policies,flags" -H "Authorization: Bearer $TOKEN")
check "GET /v1/tenants/{id}/snapshot?include=policies,flags" "200" "$HTTP"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/tenants/$DEMO_TID/policies" -H "Authorization: Bearer $TOKEN")
check "GET /v1/tenants/{id}/policies" "200" "$HTTP"
HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/v1/tenants/$DEMO_TID/flags" \
  -H "Authorization: Bearer $DEMO_TOKEN" -H "X-Tenant-Id: $DEMO_TID")
check "GET /v1/tenants/{id}/flags (consumer)" "200" "$HTTP"

echo ""
echo "--- 12. ABAC deny scenario ---"
FREE_TOKEN=$(curl -sf -X POST "$BASE/v1/dev/token" -H "Content-Type: application/json" \
  -d '{"sub":"free@test.local","tid":"00000000-0000-0000-0000-000000000002","roles":["user"],"perms":["admin:write"],"plan":"free","region":"us-east-1"}' | jq -r '.access_token')
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/v1/tenants" \
  -H "Authorization: Bearer $FREE_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Denied","plan":"free","region":"us-east-1"}')
check "ABAC deny for free plan admin:write" "403" "$HTTP"

DENY_AUDIT=$(curl -sf "$BASE/v1/audit?action=ACCESS_DENIED&size=5" -H "Authorization: Bearer $TOKEN" | jq '.totalElements')
if [ "$DENY_AUDIT" -gt 0 ] 2>/dev/null; then
  echo "  PASS: ACCESS_DENIED audit entries found ($DENY_AUDIT)"
  PASS=$((PASS + 1))
else
  echo "  FAIL: No ACCESS_DENIED audit entries"
  FAIL=$((FAIL + 1))
fi

echo ""
echo "==========================================="
echo "Results: $PASS passed, $FAIL failed"
echo "==========================================="
[ "$FAIL" -eq 0 ] && exit 0 || exit 1

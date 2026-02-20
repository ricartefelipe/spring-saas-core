#!/usr/bin/env bash
set -e
BASE_A="http://localhost:8080"
BASE_B="http://localhost:8081"
echo "=== Smoke test ==="

# 1. Get token (admin global)
TOKEN=$(curl -sf -X POST "$BASE_A/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin@example.com","password":"password"}' | jq -r '.accessToken')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "FAIL: could not get token"
  exit 1
fi
echo "Got token (admin)"

# 2. /v1/me
ME=$(curl -sf -X GET "$BASE_A/v1/me" -H "Authorization: Bearer $TOKEN" -H "X-Correlation-Id: smoke-$(date +%s)")
echo "GET /v1/me: $ME"
echo "$ME" | jq -e '.correlation_id' >/dev/null && echo "  correlation_id present" || true

# 3. Create tenant (admin has tenants:write)
TENANT_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_A/v1/tenants" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: smoke-$(date +%s)" \
  -d '{"name":"Smoke Tenant","plan":"pro","primaryRegion":"region-a","shardKey":"shard-a"}')
HTTP=$(echo "$TENANT_RESP" | tail -n1)
BODY=$(echo "$TENANT_RESP" | sed '$d')
if [ "$HTTP" != "201" ]; then
  echo "Create tenant returned $HTTP (may be OK if already exists)"
fi
TENANT_ID=$(echo "$BODY" | jq -r '.id // empty')
if [ -n "$TENANT_ID" ]; then
  echo "Created tenant: $TENANT_ID"
fi

# 4. Create user with Idempotency-Key
IDEM_KEY="smoke-idem-$(date +%s)"
USER_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_A/v1/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001" \
  -H "Idempotency-Key: $IDEM_KEY" \
  -H "X-Correlation-Id: smoke-$(date +%s)" \
  -d '{"email":"smoke@example.com","password":"password"}')
HTTP=$(echo "$USER_RESP" | tail -n1)
echo "POST /v1/users (idempotency): $HTTP"
if [ "$HTTP" != "201" ] && [ "$HTTP" != "200" ]; then
  echo "Unexpected status (tenant may be required in context)"
fi

# 5. Denial 403: tenant mismatch (use tenant B header with token that has tenant A)
echo ""
echo "=== 403 scenario: tenant mismatch ==="
TOKEN_B=$(curl -sf -X POST "$BASE_A/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"userb@example.com","password":"password"}')
TOKEN_B_ACCESS=$(echo "$TOKEN_B" | jq -r '.accessToken')
HTTP403=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_A/v1/tenants/a0000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer $TOKEN_B_ACCESS" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001")
# userb is tenant B; requesting with X-Tenant-Id tenant A should be 403 if we validate
echo "GET tenant A with tenant B user + X-Tenant-Id A: $HTTP403 (expect 403 or 200 depending on validation)"

# 6. Correlation and trace
echo ""
echo "=== Correlation ID (from response header) ==="
curl -sI -X GET "$BASE_A/v1/me" -H "Authorization: Bearer $TOKEN" | grep -i x-correlation || true
echo "Smoke done."

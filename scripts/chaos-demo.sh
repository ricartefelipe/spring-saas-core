#!/usr/bin/env bash
set -e
BASE="http://localhost:8080"
echo "=== Chaos demo ==="
TOKEN=$(curl -sf -X POST "$BASE/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin@example.com","password":"password"}' | jq -r '.accessToken')
TENANT_ID="a0000000-0000-0000-0000-000000000001"
echo "Enabling chaos for tenant..."
curl -s -X PUT "$BASE/v1/admin/chaos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d '{"latencyP50Ms":100,"latencyP95Ms":500,"errorRatePercent":10,"expires_at":"2026-12-31T00:00:00Z","createdBy":"chaos-demo"}' || true
echo "Sending requests..."
for i in 1 2 3; do
  curl -s -o /dev/null -w "%{http_code}\n" "$BASE/v1/me" -H "Authorization: Bearer $TOKEN" -H "X-Tenant-Id: $TENANT_ID"
  sleep 0.5
done
echo "Disabling chaos..."
curl -s -X PUT "$BASE/v1/admin/chaos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d '{"latencyP50Ms":0,"latencyP95Ms":0,"errorRatePercent":0,"expires_at":"2020-01-01T00:00:00Z","createdBy":"chaos-demo"}' || true
echo "Chaos demo done. Check Grafana for metrics."

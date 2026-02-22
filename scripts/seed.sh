#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
BASE="http://localhost:8080"

echo "=== Seeding via Liquibase (applied on startup) ==="
echo "Liquibase seed data is applied automatically. Verifying..."

echo ""
echo "=== Generating dev token ==="
TOKEN=$(curl -sf -X POST "$BASE/v1/dev/token" \
  -H "Content-Type: application/json" \
  -d '{
    "sub": "admin@system.local",
    "tid": "00000000-0000-0000-0000-000000000001",
    "roles": ["admin"],
    "perms": ["tenants:read","tenants:write","policies:read","policies:write","flags:read","flags:write","admin:read","admin:write"],
    "plan": "enterprise",
    "region": "global"
  }' | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "FAIL: Could not generate dev token. Is the app running with profile=local?"
  exit 1
fi
echo "Dev token generated successfully."

echo ""
echo "=== Verifying seed data ==="
echo "Tenants:"
curl -sf "$BASE/v1/tenants?size=10" -H "Authorization: Bearer $TOKEN" | jq '.content[] | {id, name, plan, region, status}'

echo ""
echo "Policies:"
curl -sf "$BASE/v1/policies?size=10" -H "Authorization: Bearer $TOKEN" | jq '.content[] | {id, permissionCode: .permissionCode, effect}'

echo ""
echo "Feature Flags (Demo Corp):"
curl -sf "$BASE/v1/tenants/00000000-0000-0000-0000-000000000002/flags" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: 00000000-0000-0000-0000-000000000002" | jq '.[] | {name, enabled, rolloutPercent}'

echo ""
echo "Seed verification complete."

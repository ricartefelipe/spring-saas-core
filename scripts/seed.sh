#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
echo "Migrations and seeds are applied on app startup. Ensure stack is up:"
curl -sf http://localhost:8080/readyz && echo " App A OK" || true
curl -sf http://localhost:8081/readyz && echo " App B OK" || true
echo "Seed users: admin@example.com (tenant A, global admin), userb@example.com (tenant B). Password: password"

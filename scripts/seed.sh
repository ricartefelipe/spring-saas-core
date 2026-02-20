#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
# Migrations and seed run automatically on app startup (Liquibase with contexts shard-a/shard-b).
# So we just ensure apps are up and optionally trigger a reload.
echo "Migrations and seeds are applied on app startup. Ensure stack is up:"
curl -sf http://localhost:8080/readyz && echo " App A OK" || true
curl -sf http://localhost:8081/readyz && echo " App B OK" || true
echo "Seed users: admin@example.com (tenant A, global admin), userb@example.com (tenant B). Password: password"

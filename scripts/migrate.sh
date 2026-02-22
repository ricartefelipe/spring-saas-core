#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
echo "Running Liquibase migrations..."
echo "Migrations are applied automatically on app startup via Spring Boot."
echo "If you need to run them manually, use:"
echo "  ./mvnw liquibase:update -Dliquibase.url=jdbc:postgresql://localhost:5432/saascore -Dliquibase.username=saascore -Dliquibase.password=saascore"
echo ""
echo "Checking if app is running and DB is migrated..."
if curl -sf http://localhost:8080/actuator/health/readiness >/dev/null 2>&1; then
  echo "App is running and DB is ready (migrations applied)."
else
  echo "App is not running. Start it with ./scripts/up.sh"
  exit 1
fi

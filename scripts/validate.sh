#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
echo "=== 1. Build e testes ==="
./mvnw -q clean test -DskipTests=false
echo "=== 2. Spotless ==="
./mvnw -q spotless:check
echo "=== 3. Package ==="
./mvnw -q package -DskipTests
echo "=== 4. Docker build ==="
docker build -f docker/app.Dockerfile -t spring-saas-core:validate .
echo "=== 5. Subir stack (opcional: docker compose up -d) ==="
echo "    Rode: docker compose up -d --build"
echo "    Depois: ./scripts/seed.sh && ./scripts/smoke.sh"
echo "Validação concluída."

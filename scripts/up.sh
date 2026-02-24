#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
echo "Starting infrastructure..."
docker compose up -d --build
echo "Waiting for app to be ready..."
for i in $(seq 1 90); do
  if curl -sf http://localhost:8080/actuator/health/liveness >/dev/null 2>&1; then
    echo "App is ready!"
    break
  fi
  if [ "$i" -eq 90 ]; then
    echo "TIMEOUT: App did not start within 3 minutes"
    docker compose logs app | tail -50
    exit 1
  fi
  sleep 2
done
echo ""
echo "URLs:"
echo "  App:         http://localhost:8080"
echo "  Swagger UI:  http://localhost:8080/docs"
echo "  OpenAPI:     http://localhost:8080/v3/api-docs"
echo "  Health:      http://localhost:8080/actuator/health"
echo "  Prometheus:  http://localhost:8080/actuator/prometheus"
echo "  Grafana:     http://localhost:3030 (admin/admin)"
echo "  RabbitMQ:    http://localhost:15672 (guest/guest)"

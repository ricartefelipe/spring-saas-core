#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
docker compose up -d --build
echo "Waiting for services to be ready..."
for i in $(seq 1 60); do
  if curl -sf http://localhost:8080/readyz >/dev/null 2>&1; then
    echo "App A is ready."
    break
  fi
  sleep 2
done
echo ""
echo "URLs:"
echo "  App A (region-a): http://localhost:8080"
echo "  App B (region-b): http://localhost:8081"
echo "  Swagger A:        http://localhost:8080/docs"
echo "  Swagger B:        http://localhost:8081/docs"
echo "  Grafana:          http://localhost:3000 (admin/admin)"
echo "  Prometheus:       http://localhost:9090"
echo "  RabbitMQ UI:      http://localhost:15672 (guest/guest)"
echo "  Kafka:            localhost:9092"

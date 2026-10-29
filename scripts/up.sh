#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."

docker network inspect fluxe_shared >/dev/null 2>&1 || docker network create fluxe_shared

# O compose constroi a imagem a partir de docker/app.local.Dockerfile, que copia o JAR do host.
# Sem este passo, um checkout limpo falhava em "lstat /target: no such file or directory": a imagem
# nao era criada, nada passava a escutar na porta da aplicacao e o smoke batia no que estivesse ali.
echo "Building application jar..."
./mvnw -q -B -DskipTests package
jar=$(ls target/spring-saas-core-*.jar 2>/dev/null | head -1)
if [ -z "$jar" ]; then
  echo "ERRO: mvnw package nao produziu target/spring-saas-core-*.jar" >&2
  exit 1
fi
echo "Jar: $jar"

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

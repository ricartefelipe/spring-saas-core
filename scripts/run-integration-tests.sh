#!/usr/bin/env bash
# Testes de integração (Testcontainers: PostgreSQL) — Phase1IntegrationTest, AbacIntegrationTest
# Requisito: o daemon Docker a responder a `docker info` (não basta o binário do cliente).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# Usa o mesmo endpoint que o CLI `docker` (contexto ativo)
if command -v docker >/dev/null 2>&1; then
  ctx="${DOCKER_CONTEXT:-$(docker context show 2>/dev/null || echo default)}"
  if endpoint="$(docker context inspect -f '{{.Endpoints.docker.Host}}' "$ctx" 2>/dev/null)"; then
    if [[ -n "$endpoint" && "$endpoint" != "<no value>" && "$endpoint" != "<nil>" ]]; then
      export DOCKER_HOST="$endpoint"
    fi
  fi
fi

if ! docker info >/dev/null 2>&1; then
  echo "O daemon Docker não respondeu a 'docker info'." >&2
  echo "Inicie o motor (ou o Docker Desktop) e confirme: docker run --rm hello-world" >&2
  echo "Em Linux, muitas vezes é preciso pertencer ao grupo 'docker':" >&2
  echo "  sudo usermod -aG docker \"\$USER\"   # em seguida, novo login" >&2
  echo "Endpoint atual (contexto): ${DOCKER_HOST:-[não definido]}" >&2
  echo "Pode ainda exportar DOCKER_HOST manualmente (o mesmo de 'docker context inspect')." >&2
  exit 1
fi

chmod +x ./mvnw
exec ./mvnw -B test -Dtest='Phase1IntegrationTest,AbacIntegrationTest'

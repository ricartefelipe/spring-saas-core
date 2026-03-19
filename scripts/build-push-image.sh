#!/usr/bin/env bash
# Build e push da imagem Docker para o GHCR (deploy manual, sem GitHub Actions).
# Uso: ./scripts/build-push-image.sh
# Antes: docker login ghcr.io -u <user> (token com write:packages)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
IMAGE="${IMAGE:-ghcr.io/ricartefelipe/spring-saas-core:latest}"

echo "[build-push] Packaging..."
(cd "$REPO_DIR" && ./mvnw -B -q package -DskipTests)

echo "[build-push] Building image $IMAGE..."
(cd "$REPO_DIR" && docker build -f docker/app.Dockerfile -t "$IMAGE" .)

echo "[build-push] Pushing $IMAGE ..."
docker push "$IMAGE"

echo "[build-push] Done. Run deploy no fluxe-b2b-suite: ./scripts/deploy-manual.sh"

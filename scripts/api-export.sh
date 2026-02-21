#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
mkdir -p docs/api
curl -sf http://localhost:8080/v3/api-docs -o docs/api/openapi.json
echo "Exported docs/api/openapi.json"
if command -v yq >/dev/null 2>&1; then
  yq -P docs/api/openapi.json > docs/api/openapi.yaml
  echo "Exported docs/api/openapi.yaml"
else
  echo "Install yq to get openapi.yaml"
fi

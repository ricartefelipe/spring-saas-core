#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
BASE="http://localhost:8080"

echo "Exporting OpenAPI spec..."
mkdir -p docs/api

curl -sf "$BASE/v3/api-docs" | jq '.' > docs/api/openapi.json
echo "  Exported docs/api/openapi.json"

curl -sf "$BASE/v3/api-docs.yaml" > docs/api/openapi.yaml
echo "  Exported docs/api/openapi.yaml"

echo "OpenAPI export complete."

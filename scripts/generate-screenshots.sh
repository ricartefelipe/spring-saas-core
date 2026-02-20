#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
mkdir -p docs/screenshots
# Placeholder PNG (1x1 transparent) as base64
B64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
for name in swagger grafana-overview trace-example sequence-main-flow; do
  echo "$B64" | base64 -d > "docs/screenshots/${name}.png"
  echo "Created docs/screenshots/${name}.png"
done

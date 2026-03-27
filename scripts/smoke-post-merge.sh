#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "[smoke] spring-saas-core post-merge smoke"
echo "[smoke] running spotless + tests"
./mvnw -B spotless:check test

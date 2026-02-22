#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
./scripts/up.sh
echo "  Kafka (bootstrap): localhost:9092"
echo "  Tópicos outbox:    saascore.tenant.events (e outros por aggregate type)"

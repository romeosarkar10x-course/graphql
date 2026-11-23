#!/usr/bin/env bash
# Start Hive Router on :4000 with router/router.yaml and the composed supergraph (inside the course image;
# bin/course fed fetches the binary). scripts/compose.sh first if a subgraph schema changed.
set -euo pipefail
cd "$(dirname "$0")/.."
exec env ROUTER_CONFIG_FILE_PATH="$PWD/router/router.yaml" SUPERGRAPH_FILE_PATH="$PWD/router/supergraph.graphql" \
  PORT="${PORT:-4000}" HOST="${HOST:-127.0.0.1}" /course/.cache/fed/bin/hive-router

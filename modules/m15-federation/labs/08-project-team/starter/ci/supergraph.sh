#!/usr/bin/env bash
# Ask each running subgraph for its SDL (_service), compose, write supergraph.graphql: what a registry does on
# every publish. compose.yaml's "supergraph" service runs it.
#   ci/supergraph.sh [url-pattern]   default http://%s:%s/graphql, filled with each subgraph's name and port
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
pattern="${1:-http://%s:%s/graphql}"
export PATH="/course/.cache/node/bin:$PATH"
mkdir -p .supergraph
{
  echo "subgraphs:"
  for s in accounts:4001 catalog:4002 reviews:4003 clubs:4004; do
    name="${s%:*}"
    port="${s#*:}"
    url="$(printf "$pattern" "$name" "$port")"
    curl -fsS "$url" -H 'Content-Type: application/json' -d '{"query":"{ _service { sdl } }"}' \
      | jq -r .data._service.sdl > ".supergraph/$name.graphqls"
    printf '  %s:\n    routing_url: %s\n    schema:\n      file: ./%s.graphqls\n' "$name" "$url" "$name"
  done
} > .supergraph/supergraph.yaml
cd compose
[[ -d node_modules ]] || npm ci --no-audit --no-fund --silent
node compose.mjs ../.supergraph/supergraph.yaml ../supergraph.graphql
echo "supergraph.graphql: $(grep -c '@join__graph(' ../supergraph.graphql) subgraphs"

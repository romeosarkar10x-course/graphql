#!/usr/bin/env bash
# Compose the supergraph the way CI does: from supergraph.yaml at the repository root, into supergraph.graphql.
# Exit 0 and write the file when every subgraph composes; exit 1 and print the errors (with their codes) when not.
#   ci/compose.sh [supergraph.yaml] [out.graphql]
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
config="$(realpath "${1:-$root/supergraph.yaml}")"
out="$(realpath -m "${2:-$root/supergraph.graphql}")"
cd "$root/compose"
[[ -d node_modules ]] || npm ci --no-audit --no-fund --silent
tmp="$(mktemp)"
if node compose.mjs "$config" "$tmp"; then
  mv "$tmp" "$out"
  echo "composed $(grep -c '@join__graph(' "$out") subgraphs into $out"
else
  rm -f "$tmp"
  echo "composition failed: the supergraph was not changed" >&2
  exit 1
fi

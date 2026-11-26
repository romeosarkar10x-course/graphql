#!/usr/bin/env bash
# Serve the Go reviews subgraph on the port given (default 4003). The course image has no Go: build the static
# binary first, from this directory: bin/course go build -o reviews .
set -euo pipefail
cd "$(dirname "$0")"
[[ -x reviews ]] || { echo "no ./reviews binary: run 'bin/course go build -o reviews .' here first" >&2; exit 1; }
exec ./reviews "${1:-4003}"

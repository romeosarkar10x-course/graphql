#!/usr/bin/env bash
# Build (once) and serve the TypeScript reviews subgraph on the port given (default 4003).
set -euo pipefail
[[ -d node_modules ]] || npm ci --no-audit --no-fund
[[ dist/server.js -nt src/server.ts ]] || npm run -s build
exec node dist/server.js "${1:-4003}"

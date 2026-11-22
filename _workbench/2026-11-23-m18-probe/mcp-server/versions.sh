#!/usr/bin/env bash
# Which MCP protocol versions the lab 18.2 server (Spring AI 2.0.2, MCP Java SDK 2.1.0) agrees to.
source /course/tools/testlib.sh; source /course/modules/m06-spring/contract/app-test.sh
cd /course/modules/m18-frontier/labs/02-mcp-server/solution; app_start java -jar target/shelfie-mcp.jar || exit 1
for v in 2024-11-05 2025-03-26 2025-06-18 2025-11-25 2026-07-28; do
  printf '%s -> ' $v; curl -s localhost:4000/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
    -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"$v\",\"capabilities\":{},\"clientInfo\":{\"name\":\"probe\",\"version\":\"1\"}}}" | jq -c '.result.protocolVersion // .error'
done

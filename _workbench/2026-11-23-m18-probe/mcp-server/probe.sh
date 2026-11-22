#!/usr/bin/env bash
# Raw MCP (streamable HTTP) handshake and a tool call with curl, against the probe server on :4100.
cd "$(dirname "$0")"
java -jar target/mcp-probe.jar > /tmp/mcp.log 2>&1 & PID=$!; trap 'kill $PID' EXIT
for _ in $(seq 60); do (exec 3<>/dev/tcp/localhost/4100) 2>/dev/null && break; sleep 0.5; done
H=(-H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream')
echo "== initialize"; curl -s -D /tmp/h.txt "${H[@]}" localhost:4100/mcp -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}'; echo
grep -i "mcp-session-id\|content-type\|HTTP/" /tmp/h.txt
SID=$(grep -i mcp-session-id /tmp/h.txt | cut -d' ' -f2 | tr -d '\r')
curl -s "${H[@]}" -H "Mcp-Session-Id: $SID" localhost:4100/mcp -d '{"jsonrpc":"2.0","method":"notifications/initialized"}' -o /dev/null -w "initialized: %{http_code}\n"
echo "== tools/list"; curl -s "${H[@]}" -H "Mcp-Session-Id: $SID" localhost:4100/mcp -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'; echo
echo "== tools/call"; curl -s "${H[@]}" -H "Mcp-Session-Id: $SID" -H 'X-Shelfie-User: u1' localhost:4100/mcp -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"club","arguments":{"id":"c1"}}}'; echo

# A tiny MCP client for tests and experiments: JSON-RPC over the Streamable HTTP transport, with
# curl and jq, so any server in any language can be checked the same way. Source after tools/testlib.sh.
# It asks for protocol version 2025-11-25, the newest the course's server stack (MCP Java SDK 2.1.0) accepts, and
# sends the negotiated version in MCP-Protocol-Version on every later request, as the transport spec requires.
#   mcp_init <url> [header...]      initialize a session (sets MCP_URL, MCP_SID, MCP_VERSION, MCP_HEADERS); the headers are sent
#                                   with every later request of the session, e.g. "X-Shelfie-User: u1"
#   mcp_rpc <method> [params-json]  one request; prints the JSON-RPC response (result or error), from JSON or SSE
#   mcp_tools                       tools/list: the tools array
#   mcp_call <tool> [args-json]     tools/call: the result object ({content: [...], isError})
#   mcp_text <tool> [args-json]     the first text content of that result, parsed as JSON when it is JSON
MCP_URL= MCP_SID= MCP_VERSION= MCP_ID=0 MCP_HEADERS=()
mcp_post() { # body -> response body, headers in /tmp/mcp-headers.txt
  local h=(-H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream') x
  [[ -n $MCP_SID ]] && h+=(-H "Mcp-Session-Id: $MCP_SID")
  [[ -n $MCP_VERSION ]] && h+=(-H "MCP-Protocol-Version: $MCP_VERSION")
  for x in "${MCP_HEADERS[@]}"; do h+=(-H "$x"); done
  curl -s -m 30 -D /tmp/mcp-headers.txt "${h[@]}" "$MCP_URL" --data-binary "$1"
}
mcp_body() { # SSE (data: lines) or plain JSON -> the JSON-RPC message with an id
  local raw; raw=$(cat)
  if grep -q '^data:' <<<"$raw"; then sed -n 's/^data: \{0,1\}//p' <<<"$raw" | jq -c 'select(.id != null)' | tail -1
  else jq -c . <<<"$raw" 2>/dev/null || echo "$raw"; fi
}
mcp_init() { # url [header...]
  MCP_URL=$1; MCP_SID=; MCP_VERSION=; MCP_HEADERS=("${@:2}")
  local res
  res=$(mcp_post '{"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"course-mcp-sh","version":"1"}}}' | mcp_body)
  MCP_SID=$(grep -i '^mcp-session-id:' /tmp/mcp-headers.txt | cut -d' ' -f2 | tr -d '\r')
  MCP_VERSION=$(jq -r '.result.protocolVersion // empty' <<<"$res" 2>/dev/null)
  [[ -n $MCP_VERSION ]] || { echo "course: MCP initialize failed at $MCP_URL: $(cut -c1-200 <<<"$res")" >&2; return 1; }
  mcp_post '{"jsonrpc":"2.0","method":"notifications/initialized"}' >/dev/null
}
mcp_rpc() { # method [params]
  MCP_ID=$((MCP_ID + 1))
  mcp_post "$(jq -cn --arg m "$1" --argjson p "${2:-{\}}" --argjson id $MCP_ID '{jsonrpc: "2.0", id: $id, method: $m, params: $p}')" | mcp_body
}
mcp_tools() { mcp_rpc tools/list | jq -c '.result.tools // []'; }
mcp_call() { mcp_rpc tools/call "$(jq -cn --arg n "$1" --argjson a "${2:-{\}}" '{name: $n, arguments: $a}')" | jq -c '.result // {isError: true, content: [{type: "text", text: (.error.message // "no result")}]}'; }
mcp_text() { mcp_call "$@" | jq -r '.content[0].text // empty' | { t=$(cat); jq -c . <<<"$t" 2>/dev/null || echo "$t"; }; }

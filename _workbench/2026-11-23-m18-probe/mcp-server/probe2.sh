#!/usr/bin/env bash
# Lab 18.2's solution through contract/mcp.sh.
source /course/tools/testlib.sh; source /course/modules/m18-frontier/contract/mcp.sh
cd /course/modules/m18-frontier/labs/02-mcp-server/solution
java -jar target/shelfie-mcp.jar > /tmp/mcp2.log 2>&1 & PID=$!; trap 'kill $PID' EXIT
for _ in $(seq 60); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
mcp_init http://localhost:4000/mcp || exit 1
echo "== tools"; mcp_tools | jq -c '.[] | {name, required: .inputSchema.required, props: (.inputSchema.properties|keys)}'
echo "== list_clubs"; mcp_text list_clubs
echo "== book_details b999"; mcp_call book_details '{"id":"b999"}'
echo "== club_meetups past"; mcp_text club_meetups '{"clubId":"c1","upcomingOnly":false}'
echo "== find_books no args"; mcp_text find_books | jq -c '.books|length'
echo "== missing required"; mcp_call book_details '{}'
echo "== unknown tool"; mcp_rpc tools/call '{"name":"run_graphql","arguments":{"query":"{ hello }"}}'
echo "== session-less call"; MCP_SID=bogus mcp_rpc tools/list | cut -c1-200; cat /tmp/mcp-headers.txt | head -1

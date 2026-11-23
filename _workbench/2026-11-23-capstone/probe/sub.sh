source /course/_workbench/2026-11-23-capstone/probe/lib.sh
source /course/modules/m15-federation/contract/fed.sh
cd $R
start_jar inventory 4001 || exit 1
start_jar lending 4002 || exit 1
env ROUTER_CONFIG_FILE_PATH=router/router.yaml SUPERGRAPH_FILE_PATH=$PWD/router/supergraph.graphql PORT=4000 HOST=127.0.0.1 $FED_BIN/hive-router > /tmp/router.log 2>&1 & PIDS+=($!)
for i in $(seq 30); do curl -fs -o /dev/null localhost:4000/health && break; sleep 0.5; done
source tests/ids.env
tok() { java /course/modules/m10-security/contract/Token.java "$@" --aud toolshed-api; }
echo "--- error message passes now?"
jq -nc --arg id "$OverdueReport" '{documentId: $id}' | curl -s localhost:4000/graphql -H 'content-type: application/json' -H "Authorization: Bearer $(tok ada)" --data-binary @-; echo
echo "--- SSE subscription through router (documentId)"
jq -nc --arg id "$Availability" '{documentId: $id, variables: {toolId: "t09"}}' > /tmp/sub.json
curl -s -N -m 6 localhost:4000/graphql -H 'content-type: application/json' -H 'accept: text/event-stream' --data-binary @/tmp/sub.json > /tmp/sse.txt &
CP=$!
sleep 2
jq -nc --arg id "$BorrowTool" '{documentId: $id, variables: {input: {toolId: "t09", idempotencyKey: "s1"}}}' | curl -s localhost:4000/graphql -H 'content-type: application/json' -H "Authorization: Bearer $(tok olu)" --data-binary @- | cut -c1-120; echo
wait $CP
echo "SSE got:"; cat /tmp/sse.txt | head -10
echo "--- direct SSE on lending"
curl -s -N -m 4 localhost:4002/graphql -H 'content-type: application/json' -H 'accept: text/event-stream' --data '{"query":"subscription { availabilityChanged(toolId: \"t09\") { available } }"}' -o /tmp/sse2.txt -w "%{http_code} %{content_type}\n"; head -3 /tmp/sse2.txt
grep -i -E "subscri|error" /tmp/router.log | tail -5 | cut -c1-300

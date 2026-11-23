source /course/_workbench/2026-11-23-capstone/probe/lib.sh
source /course/modules/m15-federation/contract/fed.sh
cd $R
start_jar inventory 4001 || exit 1
start_jar lending 4002 || exit 1
compose router/supergraph.yaml router/supergraph.graphql || exit 1
env ROUTER_CONFIG_FILE_PATH=router/router.yaml SUPERGRAPH_FILE_PATH=$PWD/router/supergraph.graphql PORT=4000 HOST=127.0.0.1 $FED_BIN/hive-router > /tmp/router.log 2>&1 & PIDS+=($!)
for i in $(seq 30); do curl -fs -o /dev/null localhost:4000/health && break; sleep 0.5; done
curl -s localhost:4000/health; echo
source tests/ids.env
tok() { java /course/modules/m10-security/contract/Token.java "$@" --aud toolshed-api; }
ADA="Authorization: Bearer $(tok ada)"
pd() { # id vars [header]
  jq -nc --arg id "$1" --argjson v "${2:-{\}}" '{documentId: $id, variables: $v}' | curl -s -D /tmp/h.txt localhost:4000/graphql -H 'content-type: application/json' ${3:+-H "$3"} --data-binary @-; echo; grep -i -E "^HTTP|cost" /tmp/h.txt | tr -d '\r'
}
echo "--- raw query refused"; q localhost:4000/graphql "{ __typename }"
echo "--- ToolPage"; pd $ToolPage '{"first": 3, "filter": {"powered": true}}' | cut -c1-400
echo "--- ToolPage 50"; pd $ToolPage '{"first": 50}' | tail -3
echo "--- MyLoans as ada"; pd $MyLoans '{}' "$ADA" | cut -c1-400
echo "--- OverdueReport 50x50 as root"; pd $OverdueReport '{"first": 50, "history": 50}' "Authorization: Bearer $(tok root)" | cut -c1-300
echo "--- OverdueReport default as root"; pd $OverdueReport '{}' "Authorization: Bearer $(tok root)" | cut -c1-300
echo "--- unknown id"; pd deadbeef '{}'
echo "--- expired token"; pd $MyLoans '{}' "Authorization: Bearer $(tok ada --expired)" | cut -c1-300
echo "--- borrow"; pd $BorrowTool '{"input": {"toolId": "t03", "idempotencyKey": "r1"}}' "$ADA" | cut -c1-300
grep -i -E "warn|error" /tmp/router.log | head -5 | cut -c1-300
echo "--- forged / none / wrong aud"
for t in "$(tok ada --forged)" "$(tok ada --none)" "$(java /course/modules/m10-security/contract/Token.java ada)"; do pd $MyLoans '{}' "Authorization: Bearer $t" | head -2 | cut -c1-200; done
grep -i jwt /tmp/router.log | head -5 | cut -c1-400

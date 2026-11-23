source /course/_workbench/2026-11-23-capstone/probe/lib.sh
cd $R && mvn -q -DskipTests package 2>&1 | grep ERROR | head -3
start_jar inventory 4001 || exit 1
start_jar lending 4002 || exit 1
sed 's/^headers:/query_planner:\n  allow_expose: true\n\nheaders:/' router/router.yaml > router/router-plan.yaml
env ROUTER_CONFIG_FILE_PATH=$PWD/router/router-plan.yaml SUPERGRAPH_FILE_PATH=$PWD/router/supergraph.graphql PORT=4000 HOST=127.0.0.1 /course/.cache/fed/bin/hive-router > /tmp/router.log 2>&1 & PIDS+=($!)
for i in $(seq 30); do curl -fs -o /dev/null localhost:4000/health && break; sleep 0.5; done
source tests/ids.env
pd() { jq -nc --arg id "$1" --argjson v "${2:-{\}}" '{documentId: $id, variables: $v}'; }
echo "--- cost of ToolPage at 12 (default) and MyLoans"
pd $ToolPage | curl -s -D - -o /dev/null localhost:4000/graphql -H 'content-type: application/json' --data-binary @- | grep -i x-cost-estimated
echo "--- plan of ToolPage"
pd $ToolPage '{"first": 30}' | curl -s localhost:4000/graphql -H 'content-type: application/json' -H 'hive-expose-query-plan: true' --data-binary @- > /tmp/plan.json
jq -c '.extensions | keys' /tmp/plan.json; jq -c '[.. | objects | select(.kind? == "Fetch") | {serviceName, op: (.operation // "" | .[0:80])}]' /tmp/plan.json
echo "--- lending down"
kill ${PIDS[1]}; wait ${PIDS[1]} 2>/dev/null; sleep 1
pd $ToolPage '{"first": 3}' | curl -s localhost:4000/graphql -H 'content-type: application/json' --data-binary @- | cut -c1-500; echo
rm -f router/router-plan.yaml

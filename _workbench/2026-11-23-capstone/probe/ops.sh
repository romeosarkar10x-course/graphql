source /course/_workbench/2026-11-23-capstone/probe/lib.sh
source /course/modules/m16-production/contract/ops.sh
trap 'stop_all; ops_stop' EXIT
cd $R && mvn -q -DskipTests package 2>&1 | grep ERROR | head -3
OPS_DASHBOARDS=$R/ops ops_start || exit 1
start_jar inventory 4001 || exit 1
start_jar lending 4002 || exit 1
scripts/router.sh > /tmp/router.log 2>&1 & PIDS+=($!)
for i in $(seq 30); do curl -fs -o /dev/null localhost:4000/health && break; sleep 0.5; done
tok() { java /course/modules/m10-security/contract/Token.java "$@" --aud toolshed-api; }
for i in 1 2 3; do hurl --test --variables-file tests/ids.env --variable api=http://localhost:4000/graphql \
  --variable token_member=$(tok ada) --variable token_other=$(tok olu) --variable token_admin=$(tok root) --variable token_expired=$(tok ada --expired) --variable run=$RANDOM tests/api.hurl >/dev/null 2>&1; echo "hurl run $i: $?"; done
sleep 8
echo "--- trace by id"
source tests/ids.env
tid=$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n'); sid=$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')
jq -nc --arg id "$MyLoans" '{documentId: $id}' | curl -s localhost:4000/graphql -H 'content-type: application/json' -H "traceparent: 00-$tid-$sid-01" -H "Authorization: Bearer $(tok ada)" --data-binary @- | cut -c1-100; echo
svc() { tempo_trace $tid 2>/dev/null | jq -c '[.trace.resourceSpans[]?.resource.attributes[]? | select(.key=="service.name") | .value.stringValue] | unique' 2>/dev/null; }
for i in $(seq 20); do sleep 1; echo "$i: $(svc)"; [[ $(svc) == *router* ]] && break; done
tempo_trace $tid | jq -c '[.trace.resourceSpans[] | {svc: (.resource.attributes[] | select(.key=="service.name") | .value.stringValue), spans: [.scopeSpans[].spans[].name]}]' | cut -c1-600
echo "--- search after 25s"
sleep 25; traceql '{}' 10 | jq -c '.[] | {rootServiceName, rootTraceName}' | sort | uniq -c | head

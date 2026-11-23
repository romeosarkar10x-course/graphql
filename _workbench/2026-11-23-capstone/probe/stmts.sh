source /course/_workbench/2026-11-23-capstone/probe/lib.sh
cd $R && mvn -q -DskipTests package 2>&1 | grep ERROR | head -3
start_jar inventory 4001 || exit 1
start_jar lending 4002 || exit 1
scripts/router.sh > /tmp/router.log 2>&1 & PIDS+=($!)
for i in $(seq 30); do curl -fs -o /dev/null localhost:4000/health && break; sleep 0.5; done
source tests/ids.env
T=$(java /course/modules/m10-security/contract/Token.java ada --aud toolshed-api)
run() { jq -nc --arg id "$2" --argjson v "$3" '{documentId: $id, variables: $v}' | curl -s localhost:4000/graphql -H 'content-type: application/json' ${4:+-H "Authorization: Bearer $4"} --data-binary @- > /tmp/r.json; }
for spec in "ToolPage|$ToolPage|{\"first\":12}|" "ToolPage-filter|$ToolPage|{\"first\":12,\"filter\":{\"powered\":true},\"sort\":\"NEWEST\"}|" "ToolDetail|$ToolDetail|{\"by\":{\"barcode\":\"TS-0005\"}}|" "MyLoans|$MyLoans|{}|$T"; do
  IFS='|' read name id vars tok <<<"$spec"
  run x $id "$vars" "$tok"   # warm
  a=$(stmts 4001); b=$(stmts 4002); run x $id "$vars" "$tok"; c=$(stmts 4001); d=$(stmts 4002)
  echo "$name: inventory $(jq -n "$c-$a"), lending $(jq -n "$d-$b") $(head -c 60 /tmp/r.json)"
done
for c in 1 8; do java scripts/Load.java --seconds 8 --concurrency $c --token $T | head -1; done
grep -o -i 'dedup[a-z_]*' /tmp/router.log | sort | uniq -c | head

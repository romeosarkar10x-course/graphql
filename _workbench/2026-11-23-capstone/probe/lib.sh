# Probe helpers for the capstone build (inside the course image). Source it.
source /course/tools/testlib.sh
R=/course/modules/m19-capstone/reference
PIDS=()
stop_all() { for p in "${PIDS[@]}"; do kill $p 2>/dev/null; done; for p in "${PIDS[@]}"; do wait $p 2>/dev/null; done; PIDS=(); }
trap stop_all EXIT
start_jar() { # name port [args]
  local n=$1 port=$2; shift 2
  java -XX:TieredStopAtLevel=1 -Xmx256m -jar $R/$n/target/$n.jar "$@" > /tmp/$n.log 2>&1 & PIDS+=($!)
  for i in $(seq 90); do curl -fs -o /dev/null localhost:$port/actuator/health && return 0; kill -0 ${PIDS[-1]} 2>/dev/null || break; sleep 1; done
  echo "$n did not start:"; grep -m3 -E "Caused by|ERROR" /tmp/$n.log | cut -c1-700; return 1
}
q() { # url query [vars] [header]
  jq -n --arg q "$2" --argjson v "${3:-{\}}" '{query:$q,variables:$v}' | curl -s "$1" -H 'content-type: application/json' ${4:+-H "$4"} --data-binary @-; echo
}
stmts() { curl -s localhost:$1/actuator/metrics/toolshed.db.statements | jq '.measurements[0].value'; }

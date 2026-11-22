#!/usr/bin/env bash
# Mini-project 16.10: your dashboard.json, imported into Grafana, each required panel's PromQL run against
# Prometheus while the reference Shelfie serves known traffic; then the runbook.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m16-production/contract/ops.sh
source /course/modules/m16-production/contract/lab-app.sh
cd "$LAB_DIR" || exit 1
trap 'app_stop; kill ${LOADS[*]} 2>/dev/null; ops_stop' EXIT
[[ -f dashboard.json ]] && jq -e '.panels | type == "array"' dashboard.json >/dev/null 2>&1 || { fail "dashboard.json: a Grafana dashboard with panels"; finish; }
ops_start prometheus grafana || finish
reviews_start || finish
REF_JAR=$(ref_app_jar) || finish
app_start java -jar "$REF_JAR" --management.tracing.enabled=false --logging.level.root=warn || finish
for _ in $(seq 1 60); do ops_up http://localhost:4000/actuator/health && break; sleep 0.5; done
imp=$(jq '{dashboard: (. + {id: null}), overwrite: true}' dashboard.json | curl -sS -X POST localhost:3000/api/dashboards/db -H 'Content-Type: application/json' -d @-)
[[ $(jq -r .status <<<"$imp" 2>/dev/null) == success ]] && pass "Grafana imports dashboard.json" || fail "Grafana refused dashboard.json" "$(cut -c1-200 <<<"$imp")"
# Known traffic for 35 s: web's club page (most requests), a partner's expensive query (most cost), bad input from
# android, invalid documents from ios, and the reviews service failing (REVIEWS_UNAVAILABLE).
reviews_fault error
LOADS=()
java -cp "$LIB/*" "$OPS/Peak.java" --seconds 35 --rate 20 >/dev/null 2>&1 & LOADS+=($!)
EXP='{"operationName":"Recommendations","query":"query Recommendations { books(first: 10) { title similar(first: 5) { title similar(first: 5) { title reviews { body } } } } }"}'
loop() { local end=$(( $(date +%s) + 35 )); while (( $(date +%s) < end )); do gql_post "$1" "graphql-client-name: $2" >/dev/null 2>&1; sleep "$3"; done; }
for _ in 1 2 3 4; do loop "$EXP" partner-bulk 0.2 & LOADS+=($!); done
loop '{"operationName":"Rate","query":"mutation Rate { addReview(bookId: \"b1\", rating: 9, body: \"x\") { id } }"}' android 0.5 & LOADS+=($!)
loop '{"query":"{ nope }"}' ios 1 & LOADS+=($!)
sleep 33
panel() { jq -r --arg re "$1" '[.panels[], (.panels[] | .panels[]?)] | map(select(.title | test($re; "i"))) | .[0].targets[0].expr // empty' dashboard.json \
  | sed 's/\$__rate_interval/1m/g; s/\$__interval/1m/g; s/\$__range/5m/g; s/\${__rate_interval}/1m/g'; }
top() { promq "$1" | jq -r 'max_by(.value[1] | tonumber) | .metric | del(.__name__, .instance, .job) | to_entries | map(.value) | join(",")'; }
check_top() {   # <title regex> <what> <expected label value>
  local e; e=$(panel "$1")
  if [[ -z $e ]]; then fail "a panel titled like /$1/ with a PromQL target"; return; fi
  local t; t=$(top "$e" 2>/dev/null)
  [[ ",$t," == *",$3,"* ]] && pass "$2: the top series is $3" || fail "$2: expected $3 on top" "query: $e; top series: ${t:-none}"
}
check_top 'top.*operation|operation.*top' "Top operations" ClubPage
check_top 'slow.*field|field.*slow' "Slowest fields" Book.similar
check_top 'cost.*client|client.*cost' "Cost per client" partner-bulk
e=$(panel 'error.*code|code.*error')
if [[ -n $e ]]; then
  codes=$(promq "$e" | jq -r '[.[] | .metric | del(.__name__, .instance, .job) | to_entries[].value] | unique | join(",")')
  miss=(); for c in REVIEWS_UNAVAILABLE BAD_USER_INPUT ValidationError; do [[ ",$codes," == *",$c,"* ]] || miss+=("$c"); done
  [[ ${#miss[@]} == 0 ]] && pass "Error rate by code: REVIEWS_UNAVAILABLE, BAD_USER_INPUT and ValidationError apart" || fail "Error rate by code: missing ${miss[*]}" "query: $e; got: $codes"
else fail "a panel titled like /error.*code/ with a PromQL target"; fi
wait "${LOADS[@]}" 2>/dev/null
if require_file RUNBOOK.md "the on-call runbook (rubric)"; then
  a=$(grep -ciE '^#+ .*(alert|when|symptom|page)' RUNBOOK.md); q=$(grep -cE '(rate|sum|histogram_quantile|topk|increase)\(' RUNBOOK.md)
  (( a >= 3 && q >= 3 )) && pass "the runbook: $a alerts or symptoms, $q PromQL conditions" || fail "RUNBOOK.md: at least 3 alert sections, each with a PromQL condition" "found $a sections, $q queries"
  notes_check RUNBOOK.md 250 m16/10
fi
finish

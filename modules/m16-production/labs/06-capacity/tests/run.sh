#!/usr/bin/env bash
# Lab 16.6: the cost of each operation (extension + metric), the per-pod cost rate in PromQL against a real
# Prometheus, an HPA on it simulated over 25 minutes of traffic, and the capacity notes.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m16-production/contract/ops.sh
source /course/modules/m16-production/contract/lab-app.sh
cd "$LAB_DIR" || exit 1
trap 'app_stop; ops_stop' EXIT
ops_start prometheus || finish
lab_app_up --management.tracing.enabled=false || finish
cost() { gql_post "{\"query\":\"$1\"}" | jq -r '.extensions.cost // "none"'; }
got=$(printf '%s ' "$(cost '{ books(first: 2) { title similar { title } } }')" "$(cost '{ club(id: \"c1\") { name members { name } readingList { title reviews { rating } } } }')" \
  "$(cost '{ books { similar(first: 2) { title } } }')" "$(cost '{ clubs { members { name } } }')" "$(cost '{ book(id: \"b1\") { title } }')")
[[ "$got" == "11 38 31 22 2 " ]] && pass "extensions.cost by the rule (5 operations)" || fail "extensions.cost for the five test operations" "got: $got"
for _ in $(seq 1 3); do gql_post '{"operationName":"ClubPage","query":"query ClubPage { club(id: \"c1\") { name members { name } readingList { title reviews { rating } } } }"}' "graphql-client-name: web" >/dev/null; done
M=$(curl -fsS localhost:4000/actuator/prometheus)
s=$(grep -E '^graphql_cost_sum\{' <<<"$M" | grep 'graphql_client_name="web"' | grep 'graphql_operation_name="ClubPage"' | awk '{print $2}')
[[ "$s" == 114.0 || "$s" == 114 ]] && pass "graphql_cost_sum{graphql_client_name=\"web\", graphql_operation_name=\"ClubPage\"} = 3 x 38" \
  || fail "graphql_cost_sum for web/ClubPage after three club pages (expected 114)" "got: ${s:-no series}"
Q=k8s/cost-per-second.promql
if [[ -f $Q ]]; then
  java -cp "$LIB/*" "$OPS/Peak.java" --seconds 40 --rate 20 --body "$OPS/club-page.json" >/dev/null 2>&1 & LOAD=$!
  sleep 38; v=$(promq "$(cat $Q)" | jq -r '[.[] | select(.metric.instance == "localhost:4000")][0].value[1] // "none"'); wait $LOAD
  awk -v v="$v" 'BEGIN { exit !(v+0 >= 800 && v+0 <= 1600) }' && pass "the PromQL gives cost per second per pod (~1,220 at 20 club pages/s): $(printf %.0f "$v")" \
    || fail "$Q at 20 club pages a second (cost 61 each) should give about 1,220 for instance localhost:4000" "got: $v"
else fail "the per-pod cost rate in $Q"; fi
H=k8s/hpa.yaml
if [[ -f $H ]] && J=$(java -cp "$LIB/*" "$OPS/Yaml.java" $H 2>/tmp/yaml.err); then
  h=$(jq -c '[.[] | select(.kind == "HorizontalPodAutoscaler")][0]' <<<"$J")
  [[ $(jq -r .apiVersion <<<"$h") == autoscaling/v2 && $(jq -r '.spec.scaleTargetRef.kind + "/" + .spec.scaleTargetRef.name' <<<"$h") == Deployment/shelfie \
     && $(jq -r '.spec.metrics[0].type + " " + .spec.metrics[0].pods.metric.name + " " + .spec.metrics[0].pods.target.type' <<<"$h") == "Pods graphql_cost_per_second AverageValue" \
     && $(jq '.spec.minReplicas // 1' <<<"$h") -ge 2 ]] \
    && pass "hpa.yaml: autoscaling/v2 on the Pods metric graphql_cost_per_second (AverageValue), at least 2 replicas" \
    || fail "$H: autoscaling/v2 HPA for Deployment shelfie, minReplicas >= 2, a Pods metric graphql_cost_per_second with target type AverageValue"
else fail "$H that parses" "$(head -2 /tmp/yaml.err 2>/dev/null)"; fi
cap=$(grep -m1 -oE '^Pod capacity: *[0-9][0-9,]*' CAPACITY.md 2>/dev/null | grep -oE '[0-9][0-9,]*' | tr -d ,)
if [[ -n $cap && -f $H ]]; then
  S=$(java -cp "$LIB/*" "$OPS/HpaSim.java" $H "$cap" 2>&1 | tail -1); note "simulated: $S"
  o=$(jq .overloadedSeconds <<<"$S" 2>/dev/null); p=$(jq .podSeconds <<<"$S" 2>/dev/null); f=$(jq .finalReplicas <<<"$S" 2>/dev/null)
  [[ -n $o ]] && (( o <= 60 )) && pass "the autoscaler keeps up: $o s with a pod over capacity (at most 60)" || fail "overloaded for ${o:-?} s of 25 min (at most 60): the target is too close to capacity, or maxReplicas too low"
  [[ -n $p ]] && (( p <= 9000 )) && (( f <= 3 )) && pass "and does not waste pods: $p pod-seconds, back to $f replicas" \
    || fail "${p:-?} pod-seconds (at most 9,000) and ${f:-?} replicas at the end (at most 3): the target is too low or it never scales down"
else fail "CAPACITY.md: a line 'Pod capacity: <N> cost/s' (your measured knee times the page's cost)"; fi
require_file CAPACITY.md "the capacity notes (rubric)" && notes_check CAPACITY.md 150 m16/06
finish

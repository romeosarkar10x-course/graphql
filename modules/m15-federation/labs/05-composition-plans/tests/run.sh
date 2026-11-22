#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m15-federation/contract/fed.sh
cd "$LAB_DIR" || exit 1
W=$(mktemp -d); trap 'rm -rf $W; fed_stop' EXIT
# 1. Composition as CI runs it.
if [[ -x ci/compose.sh ]]; then
  rm -f $W/good.graphql
  if ci/compose.sh supergraph.yaml $W/good.graphql >$W/good.log 2>&1 && grep -q 'join__Graph' $W/good.graphql 2>/dev/null; then
    pass "ci/compose.sh composes the four subgraphs and writes the supergraph"
  else fail "ci/compose.sh composes the four subgraphs and writes the supergraph" "$(tail -3 $W/good.log)"; fi
  mkdir -p $W/broken/subgraphs && cp subgraphs/*.graphqls $W/broken/subgraphs/ && cp supergraph.yaml $W/broken/
  sed -i -E 's/^(type Book[^{]*)@key\(fields: *"id"\) */\1/' $W/broken/subgraphs/catalog.graphqls
  echo "keep me" > $W/old.graphql
  if ci/compose.sh $W/broken/supergraph.yaml $W/old.graphql >$W/bad.log 2>&1; then
    fail "ci/compose.sh fails on a schema that does not compose" "it exited 0 with catalog's Book @key(fields: \"id\") removed"
  elif ! grep -qE '[A-Z]+_[A-Z_]+' $W/bad.log; then
    fail "ci/compose.sh fails on a schema that does not compose" "it failed, but printed no composition error code: $(tail -2 $W/bad.log)"
  elif [[ $(cat $W/old.graphql) != "keep me" ]]; then
    fail "ci/compose.sh fails on a schema that does not compose" "it overwrote the last good supergraph"
  else pass "ci/compose.sh fails on a broken schema, names the error ($(grep -oE '[A-Z]+_[A-Z_]+' $W/bad.log | head -1)) and keeps the last good supergraph"; fi
else fail "ci/compose.sh exists and is executable"; fi
wf=.github/workflows/supergraph.yml
if [[ -f $wf ]]; then
  miss=()
  grep -q 'pull_request' $wf || miss+=("runs on pull_request")
  grep -q 'subgraphs/' $wf || miss+=("only when subgraphs/ change (paths)")
  grep -q 'actions/setup-node' $wf || miss+=("sets up Node.js")
  grep -q 'ci/compose.sh' $wf || miss+=("runs ci/compose.sh")
  grep -q 'actions/upload-artifact' $wf || miss+=("keeps supergraph.graphql as an artifact")
  (( ${#miss[@]} == 0 )) && pass "$wf composes every pull request that touches a subgraph" || fail "$wf" "missing: $(IFS=';'; echo "${miss[*]}")"
else fail "$wf exists"; fi
# 2. Predictions against the router's own plans.
compose supergraph.yaml $W/sg.graphql 2>/dev/null || compose $FED/compose/supergraph.yaml $W/sg.graphql
subs_start && router_start apollo $W/sg.graphql || { fail "subgraphs and router start"; finish; }
[[ -f predictions.json ]] && jq -e . predictions.json >/dev/null 2>&1 || { fail "predictions.json is valid JSON"; finish; }
right=0
for q in q1 q2 q3 q4 q5 n1; do
  actual=$(plan_steps apollo http://127.0.0.1:4000/ "$(grep -v '^#' queries/$q.graphql)" | jq -c .steps)
  mine=$(jq -c --arg q $q '.[$q] // [] | map(sort)' predictions.json)
  if [[ "$mine" == "$actual" ]]; then right=$((right+1)); pass "$q: predicted plan $mine"
  else fail "$q: predicted $mine" "the router's plan: $actual"; fi
done
for p in 4001 4002 4003 4004; do sub_reset $p; done
gql http://127.0.0.1:4000/ "$(grep -v '^#' queries/n1.graphql)" > /dev/null
calls=$(for p in 4001 4002 4003 4004; do sub_stats $p | jq -r '"\(.subgraph) \(.requests)"'; done | paste -sd, -)
note "n1 cost per subgraph (HTTP requests): $calls"
notes_check notes.md 100 m15/05
finish

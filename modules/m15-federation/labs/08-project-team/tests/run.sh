#!/usr/bin/env bash
# Mini-project 15.8: the four subgraphs from your jar, composed from what they serve (_service), behind Apollo
# Router with your router.yaml; then the feature, and the compose file that runs it all in one command.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m15-federation/contract/fed.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-team.jar
W=$(mktemp -d); trap 'rm -rf $W; fed_stop' EXIT
app_build $JAR || finish
export SUB_JAR=$PWD/$JAR
for s in accounts:4001 catalog:4002 reviews:4003 clubs:4004; do
  sub_start ${s%:*} ${s#*:} || { fail "subgraph ${s%:*} starts" "$(grep -m2 -E 'Caused by|ERROR' /tmp/sub-${s%:*}.log | cut -c1-250)"; finish; }
done
pass "the four subgraphs start from $JAR"
{ echo "subgraphs:"
  for s in accounts:4001 catalog:4002 reviews:4003 clubs:4004; do
    gql http://localhost:${s#*:}/graphql '{ _service { sdl } }' | jq -r '.data._service.sdl // empty' > $W/${s%:*}.graphqls
    printf '  %s:\n    routing_url: http://localhost:%s/graphql\n    schema:\n      file: %s\n' ${s%:*} ${s#*:} $W/${s%:*}.graphqls
  done; } > $W/supergraph.yaml
if compose $W/supergraph.yaml $W/supergraph.graphql 2>$W/errors.txt; then pass "the four subgraphs compose (from _service)"
else fail "the four subgraphs compose (from _service)" "$(head -3 $W/errors.txt | cut -c1-300)"; finish; fi
[[ -f router.yaml ]] || { fail "router.yaml exists"; finish; }
sed 's/listen: 0.0.0.0:8088/listen: 127.0.0.1:8088/' router.yaml > $W/router.yaml
router_start apollo $W/supergraph.graphql $W/router.yaml || { fail "Apollo Router starts with your router.yaml"; finish; }
URL=http://127.0.0.1:4000/
app_hurl $LAB_HOME/tests/team.hurl "club book of the month (team.hurl)"
# One command: compose.yaml (the checkpoint runs it with docker compose on your machine).
if [[ -f compose.yaml ]]; then
  miss=()
  for s in accounts catalog reviews clubs router; do grep -qE "^  $s:" compose.yaml || miss+=("service $s"); done
  grep -q '4000:4000' compose.yaml || miss+=("router published on 4000")
  grep -q 'graphql-course:1' compose.yaml || miss+=("the course image")
  grep -q 'service_healthy\|service_completed_successfully' compose.yaml || miss+=("start order (depends_on conditions)")
  (( ${#miss[@]} == 0 )) && pass "compose.yaml runs the stack in one command" || fail "compose.yaml runs the stack" "missing: $(IFS=';'; echo "${miss[*]}")"
else fail "compose.yaml exists"; fi
require_file RETRO.md "the retrospective (rubric)" && notes_check RETRO.md 150 m15/08
finish

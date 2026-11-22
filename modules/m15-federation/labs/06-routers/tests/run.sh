#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m15-federation/contract/fed.sh
cd "$LAB_DIR" || exit 1
W=$(mktemp -d); trap 'rm -rf $W; fed_stop' EXIT
compose $FED/compose/supergraph.yaml $W/sg.graphql && subs_start || { fail "subgraphs start"; finish; }
stop_router() { kill ${FED_PIDS[-1]} 2>/dev/null; wait ${FED_PIDS[-1]} 2>/dev/null; unset 'FED_PIDS[-1]'; }
url() { [[ $1 == apollo ]] && echo http://127.0.0.1:4000/ || echo http://127.0.0.1:4000/graphql; }
declare -A loaded
for k in apollo hive; do
  f=$k.yaml
  [[ -f $f ]] || { fail "$f exists"; continue; }
  router_start $k $W/sg.graphql $PWD/$f || { fail "$k router starts with $f" "$(grep -m2 -iE 'error|invalid' /tmp/router-$k.log | cut -c1-300)"; continue; }
  u=$(url $k); sub_reset 4001
  r=$(gql $u '{ me { handle } }' '{}' 'X-Shelfie-User: u2' 'Authorization: Bearer course-token' 'Cookie: session=secret')
  h=$(sub_stats 4001 | jq -c .lastHeaders)
  if [[ $(jq -r .data.me.handle <<<"$r") == benok && $(jq -r '.authorization' <<<"$h") == "Bearer course-token" && $(jq -r '.cookie // empty' <<<"$h") == "" ]]; then
    pass "$k: X-Shelfie-User and Authorization reach the subgraphs, Cookie does not"
  else fail "$k: X-Shelfie-User and Authorization reach the subgraphs, Cookie does not" "me: $(jq -c .data <<<"$r"); accounts saw: $h"; fi
  r=$(gql $u '{ __schema { queryType { name } } }')
  [[ $(jq -r '.data.__schema.queryType.name // empty' <<<"$r") == "" && $(jq '.errors | length' <<<"$r") -gt 0 ]] \
    && pass "$k: introspection is off" || fail "$k: introspection is off" "$(cut -c1-200 <<<"$r")"
  r=$(gql $u '{ book(id: "b1") { title } }' '{}' 'Apollo-Expose-Query-Plan: true' 'hive-expose-query-plan: true')
  [[ $(jq -r '.extensions // {} | has("apolloQueryPlan") or has("queryPlan")' <<<"$r") == false ]] \
    && pass "$k: query plans are not exposed to clients" || fail "$k: query plans are not exposed to clients"
  loaded[$k]=1; stop_router
done
# A slow subgraph: reviews answers after 3 s. The page must come back within 2.5 s with the book and an error.
fed_stop; sub_start accounts 4001 && sub_start catalog 4002 && sub_start reviews 4003 --shelfie.delay-ms=3000 && sub_start clubs 4004 || { fail "subgraphs restart"; finish; }
for k in apollo hive; do
  [[ -n ${loaded[$k]:-} ]] || continue
  router_start $k $W/sg.graphql $PWD/$k.yaml || continue
  t0=$(date +%s%N); r=$(gql $(url $k) '{ book(id: "b1") { title averageRating } }'); ms=$(( ($(date +%s%N) - t0) / 1000000 ))
  if (( ms < 2500 )) && [[ $(jq -r .data.book.title <<<"$r") == "The Left Hand of Darkness" && $(jq '.errors | length' <<<"$r") -gt 0 ]]; then
    pass "$k: a 3 s subgraph times out at your limit (${ms} ms), the book still arrives"
  else fail "$k: a 3 s subgraph times out within 2.5 s, the book still arrives" "took ${ms} ms: $(cut -c1-200 <<<"$r")"; fi
  stop_router
done
# The experiment: the same traffic through both (printed for your notes, not graded).
fed_stop; subs_start
Q='{ latestReviews(first: 5) { rating author { name } book { title weeksToRead } } }'
echo "  same traffic, both routers (2000 requests, 16 at a time):"
for k in apollo hive; do
  [[ -n ${loaded[$k]:-} ]] || continue
  router_start $k $W/sg.graphql $PWD/$k.yaml || continue
  printf '        %-7s %s\n' $k "$(java -cp "$(lib_cp)" $FED/Load.java $(url $k) 2000 16 "$Q" ${FED_PIDS[-1]})"
  stop_router
done
notes_check notes.md 100 m15/06
finish

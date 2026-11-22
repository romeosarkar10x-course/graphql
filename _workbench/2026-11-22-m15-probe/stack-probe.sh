#!/usr/bin/env bash
# Four contract subgraphs, composed, then the same queries through Apollo Router and Hive Router.
source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
cd /tmp && compose $FED/compose/supergraph.yaml /tmp/sg.graphql || exit 1
subs_start || exit 1
Q1='{ club(id: "c1") { name owner { name } currentBook { title weeksToRead averageRating reviews { rating author { handle name } } } } }'
Q2='{ latestReviews(first: 3) { id author { handle } book { title } } }'
Q3='{ me { name reviews { book { title } } clubs { name } } }'
for kind in apollo hive; do
  router_start $kind /tmp/sg.graphql || { tail -20 /tmp/router-$kind.log; continue; }
  for p in 4001 4002 4003 4004; do sub_reset $p; done
  echo "== $kind"
  hdr=$([[ $kind == apollo ]] && echo 'Apollo-Expose-Query-Plan: true' || echo 'hive-expose-query-plan: true')
  gql http://127.0.0.1:4000/ "$Q1" '{}' "$hdr" > /tmp/r1.json; [[ $kind == hive ]] && gql http://127.0.0.1:4000/graphql "$Q1" '{}' "$hdr" > /tmp/r1.json
  jq -c .data /tmp/r1.json; jq -c '.errors' /tmp/r1.json; jq -r '.extensions | keys' /tmp/r1.json
  for p in 4001 4002 4003 4004; do sub_stats $p | jq -c '{subgraph,requests,entityCalls,entityItems}'; done
  url=$([[ $kind == apollo ]] && echo http://127.0.0.1:4000/ || echo http://127.0.0.1:4000/graphql)
  gql $url "$Q2" | jq -c .
  gql $url "$Q3" '{}' 'X-Shelfie-User: u1' | jq -c .
  sub_stats 4001 | jq -c .lastHeaders
  cp /tmp/r1.json /course/_workbench/2026-11-22-m15-probe/plan-$kind.json
  fed_stop_router() { :; }
  kill ${FED_PIDS[-1]}; wait ${FED_PIDS[-1]} 2>/dev/null; unset 'FED_PIDS[-1]'
  ps -o rss= -p $$ >/dev/null
done

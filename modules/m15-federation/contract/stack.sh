#!/usr/bin/env bash
# Run Shelfie's federated graph by hand, inside the course image (bin/course shell), until Ctrl-C:
#
#   bash /course/modules/m15-federation/contract/stack.sh [apollo|hive] [supergraph.yaml]
#
#   apollo|hive       which router (default apollo); http://localhost:4000/ (Apollo) or /graphql (Hive)
#   supergraph.yaml   what to compose (default: the contract's four subgraphs)
#   SKIP=catalog,reviews   do not start those contract subgraphs: you run your own on their ports
#   ROUTER_CONFIG=file     your router config (default: contract/routers/<router>.yaml, which exposes query plans)
#
# Subgraphs: accounts :4001, catalog :4002, reviews :4003, clubs :4004 (GET /_stats on each).
# To see the router publish to your host: COURSE_PUBLISH=4000 bin/course shell.
source /course/tools/testlib.sh
source /course/modules/m15-federation/contract/fed.sh
kind="${1:-apollo}"
config="${2:-$FED/compose/supergraph.yaml}"
for s in accounts:4001 catalog:4002 reviews:4003 clubs:4004; do
  [[ ",${SKIP:-}," == *",${s%:*},"* ]] && { echo "skipping ${s%:*}: yours should be on :${s#*:}"; continue; }
  sub_start "${s%:*}" "${s#*:}" || exit 1
  echo "${s%:*} on :${s#*:}"
done
compose "$config" /tmp/stack-supergraph.graphql || exit 1
ROUTER_HOST=0.0.0.0 router_start "$kind" /tmp/stack-supergraph.graphql "${ROUTER_CONFIG:-}" || exit 1
[[ $kind == apollo ]] && url=http://localhost:4000/ || url=http://localhost:4000/graphql
echo "router ($kind) on $url; supergraph in /tmp/stack-supergraph.graphql; logs in /tmp/sub-*.log, /tmp/router-$kind.log"
echo "query plans: add the header $([[ $kind == apollo ]] && echo 'Apollo-Expose-Query-Plan: true' || echo 'hive-expose-query-plan: true'). Ctrl-C stops everything."
wait

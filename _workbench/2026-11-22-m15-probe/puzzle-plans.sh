source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
compose $FED/compose/supergraph.yaml /tmp/sg.graphql && subs_start && router_start apollo /tmp/sg.graphql && router_start hive /tmp/sg.graphql "" 4005 || exit 1
for q in /course/modules/m15-federation/labs/09-query-plan-puzzle/tests/puzzles/*.graphql; do
  n=$(basename $q .graphql); Q=$(grep -v '^#' $q)
  echo "$n apollo=$(plan_steps apollo http://127.0.0.1:4000/ "$Q" | jq -c .steps) hive=$(plan_steps hive http://127.0.0.1:4005/graphql "$Q" | jq -c .steps)"
done
gql http://127.0.0.1:4000/ "$(grep -v '^#' /course/modules/m15-federation/labs/09-query-plan-puzzle/tests/puzzles/p08.graphql)" | jq -c .

source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
L=/course/modules/m15-federation/labs/05-composition-plans/solution
cd $L && compose supergraph.yaml /tmp/sg.graphql || exit 1
subs_start || exit 1
router_start apollo /tmp/sg.graphql || exit 1
router_start hive /tmp/sg.graphql "" 4005 || true
for q in queries/*.graphql; do
  n=$(basename $q .graphql)
  for p in 4001 4002 4003 4004; do sub_reset $p; done
  a=$(plan_steps apollo http://127.0.0.1:4000/ "$(grep -v '^#' $q)")
  reqs=$(for p in 4001 4002 4003 4004; do sub_stats $p | jq -r '"\(.subgraph)=\(.requests)"'; done | paste -sd' ')
  h=$(plan_steps hive http://127.0.0.1:4005/graphql "$(grep -v '^#' $q)")
  echo "$n apollo=$a hive=$h requests: $reqs"
done

source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
S=/course/modules/m15-federation/labs/06-routers/solution
compose $FED/compose/supergraph.yaml /tmp/sg.graphql
sub_start accounts 4001 && sub_start catalog 4002 && sub_start reviews 4003 --shelfie.delay-ms=3000 && sub_start clubs 4004 || exit 1
for k in apollo hive; do
  router_start $k /tmp/sg.graphql $S/$k.yaml || { tail -5 /tmp/router-$k.log; continue; }
  u=http://127.0.0.1:4000/; [[ $k == hive ]] && u=http://127.0.0.1:4000/graphql
  echo "== $k"; grep -i -m3 "warn\|error" /tmp/router-$k.log | cut -c1-250
  gql $u '{ me { handle } }' '{}' 'X-Shelfie-User: u2' 'Authorization: Bearer abc' 'Cookie: session=1' | jq -c .; sub_stats 4001 | jq -c .lastHeaders
  gql $u '{ __schema { queryType { name } } }' | jq -c . | cut -c1-200
  start=$(date +%s%N); gql $u '{ book(id: "b1") { title reviewCount } }' | jq -c . | cut -c1-300; echo "took $(( ($(date +%s%N)-start)/1000000 )) ms"
  kill ${FED_PIDS[-1]}; wait ${FED_PIDS[-1]} 2>/dev/null; unset 'FED_PIDS[-1]'
done

source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
for v in v /course/modules/m15-federation/labs/04-java-subgraphs/solution; do
  cd /course/_workbench/2026-11-22-m15-probe/lab04-variants; J=$(cd $v && pwd)/target/shelfie-java-subgraphs.jar
  SUB_JAR=$J sub_start catalog 4002; SUB_JAR=$J sub_start reviews 4003; sub_start accounts 4001; sub_start clubs 4004
  compose $FED/compose/supergraph.yaml /tmp/sg.graphql 2>/dev/null || { sed 's#.*##' /dev/null; }
  router_start apollo /tmp/sg.graphql
  curl -s -X POST localhost:4002/_reset >/dev/null; gql http://127.0.0.1:4000/ '{ latestReviews(first: 10) { id book { title } } }' >/dev/null
  echo "$v R01 catalog: $(curl -s localhost:4002/_stats)"
  curl -s -X POST localhost:4003/_reset >/dev/null; gql http://127.0.0.1:4000/ '{ books(first: 5) { title reviewCount averageRating reviews { rating } } }' >/dev/null
  echo "$v R02 reviews: $(curl -s localhost:4003/_stats)"
  gql http://localhost:4003/graphql 'query($r: [_Any!]!) { _entities(representations: $r) { ... on Review { id } } }' '{"r":[{"__typename":"Review","id":"r404"}]}' | jq -c .
  fed_stop
done

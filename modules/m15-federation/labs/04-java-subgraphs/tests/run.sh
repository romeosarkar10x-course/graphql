#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m15-federation/contract/fed.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-java-subgraphs.jar
W=$(mktemp -d); trap 'rm -rf $W; fed_stop' EXIT
app_build $JAR || finish
# Yours: catalog and reviews. The contract's: accounts and clubs.
SUB_JAR=$PWD/$JAR sub_start catalog 4002 || { fail "your catalog subgraph starts" "$(grep -m3 -E 'Caused by|Unmapped|ERROR' /tmp/sub-catalog.log | cut -c1-300)"; finish; }
SUB_JAR=$PWD/$JAR sub_start reviews 4003 || { fail "your reviews subgraph starts" "$(grep -m3 -E 'Caused by|Unmapped|ERROR' /tmp/sub-reviews.log | cut -c1-300)"; finish; }
pass "your catalog and reviews subgraphs start"
sub_start accounts 4001 && sub_start clubs 4004 || { fail "contract subgraphs start"; finish; }
# Compose from what your subgraphs say about themselves (_service.sdl), as a registry would.
for p in catalog:4002 reviews:4003; do
  gql http://localhost:${p#*:}/graphql '{ _service { sdl } }' | jq -r '.data._service.sdl // empty' > $W/${p%:*}.graphqls
done
[[ -s $W/catalog.graphqls && -s $W/reviews.graphqls ]] && pass "both answer { _service { sdl } }" || fail "both answer { _service { sdl } }"
G=$FED/subgraphs/src/main/resources/graphql
cat > $W/supergraph.yaml <<YAML
subgraphs:
  accounts:
    routing_url: http://localhost:4001/graphql
    schema:
      file: $G/accounts/accounts.graphqls
  catalog:
    routing_url: http://localhost:4002/graphql
    schema:
      file: $W/catalog.graphqls
  reviews:
    routing_url: http://localhost:4003/graphql
    schema:
      file: $W/reviews.graphqls
  clubs:
    routing_url: http://localhost:4004/graphql
    schema:
      file: $G/clubs/clubs.graphqls
YAML
if compose $W/supergraph.yaml $W/supergraph.graphql 2>$W/errors.txt; then pass "your SDL composes with accounts and clubs"
else fail "your SDL composes with accounts and clubs" "$(head -3 $W/errors.txt | cut -c1-300)"; finish; fi
router_start apollo $W/supergraph.graphql || { fail "Apollo Router starts on your supergraph"; finish; }
URL=http://127.0.0.1:4000/
app_hurl $LAB_HOME/tests/subgraphs.hurl "entities, batching and the router (subgraphs.hurl)" \
  --variable router=$URL --variable catalog=http://localhost:4002 --variable reviews=http://localhost:4003
notes_check notes.md 80 m15/04
finish

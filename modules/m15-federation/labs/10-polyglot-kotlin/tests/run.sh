#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m15-federation/contract/fed.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-accounts-kotlin.jar
W=$(mktemp -d); KPID=; trap 'kill $KPID 2>/dev/null; rm -rf $W; fed_stop' EXIT
app_build $JAR || finish
java -jar $JAR 4001 > /tmp/kotlin-accounts.log 2>&1 & KPID=$!
for _ in $(seq 60); do port_open 4001 && break; sleep 0.5; done
port_open 4001 && pass "the Kotlin accounts subgraph starts on 4001" || { fail "the Kotlin accounts subgraph starts" "$(tail -3 /tmp/kotlin-accounts.log)"; finish; }
gql http://localhost:4001/graphql '{ _service { sdl } }' | jq -r '.data._service.sdl // empty' > $W/accounts.graphqls
[[ -s $W/accounts.graphqls ]] && pass "it answers { _service { sdl } }" || { fail "it answers { _service { sdl } }" "is it federated (toFederatedSchema)?"; finish; }
sub_start catalog 4002 && sub_start reviews 4003 && sub_start clubs 4004 || { fail "the Java subgraphs start"; finish; }
G=$FED/subgraphs/src/main/resources/graphql
sed -e "s#file: ../subgraphs/src/main/resources/graphql/accounts/accounts.graphqls#file: $W/accounts.graphqls#" -e "s#file: \.\./subgraphs/#file: $FED/subgraphs/#" $FED/compose/supergraph.yaml > $W/supergraph.yaml
if compose $W/supergraph.yaml $W/supergraph.graphql 2>$W/errors.txt; then pass "your Kotlin SDL composes with Java's catalog, reviews and clubs"
else fail "your Kotlin SDL composes with Java's catalog, reviews and clubs" "$(head -3 $W/errors.txt | cut -c1-300)"; finish; fi
router_start apollo $W/supergraph.graphql $LAB_HOME/tests/router.yaml || { fail "the router starts"; finish; }
URL=http://127.0.0.1:4000/
app_hurl $LAB_HOME/tests/polyglot.hurl "one graph, two languages (polyglot.hurl)" --variable accounts=http://localhost:4001/graphql
finish

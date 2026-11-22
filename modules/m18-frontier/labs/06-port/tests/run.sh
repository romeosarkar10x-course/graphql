#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m15-federation/contract/fed.sh
source /course/modules/m18-frontier/contract/port/port.sh
cd "$LAB_DIR" || exit 1
W=$(mktemp -d); trap 'rm -rf $W; fed_stop' EXIT
grep -qE 'shelfie-subgraphs\.jar|java +-jar|mvn ' serve.sh 2>/dev/null && { fail "serve.sh runs a JVM build: the port is in another language"; finish; }
port_start "$LAB_DIR" 4003 && pass "serve.sh starts your reviews subgraph on :4003" || { fail "serve.sh starts your reviews subgraph on :4003" "$(tail -5 /tmp/port.log | cut -c1-300)"; finish; }
if diff <(port_signatures http://localhost:4003/graphql) $PORT_DIR/signatures.txt >$W/sig.diff; then pass "the same API as Java's reviews subgraph (15 field signatures from introspection)"
else fail "the API differs from Java's reviews subgraph (< yours, > the contract)"; head -12 $W/sig.diff | sed 's/^/        /'; fi
gql http://localhost:4003/graphql '{ _service { sdl } }' | jq -r '.data._service.sdl // empty' > $W/reviews.graphqls
[[ -s $W/reviews.graphqls ]] && pass "it answers { _service { sdl } } (a Federation subgraph)" || { fail "it answers { _service { sdl } }" "is it built as a federated subgraph?"; finish; }
port_graph $W/reviews.graphqls; rc=$?
(( rc == 0 )) && pass "your SDL composes with Java's accounts, catalog and clubs, and Apollo Router serves it" \
  || { (( rc == 2 )) && fail "your SDL composes with Java's accounts, catalog and clubs" "$(head -3 $W/compose-errors.txt | cut -c1-300)" || fail "the Java subgraphs and the router start"; finish; }
URL=http://127.0.0.1:4000/
app_hurl $PORT_DIR/port.hurl "the contract's Hurl suite, unchanged (port.hurl)" --variable reviews=http://localhost:4003 --variable router=$URL
notes_check PORT.md 80 m18/06
finish

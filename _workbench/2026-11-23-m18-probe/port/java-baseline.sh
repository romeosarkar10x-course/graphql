#!/usr/bin/env bash
# The Java reviews subgraph against contract/port/port.hurl, and its signatures (contract/port/signatures.txt).
source /course/tools/testlib.sh; source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m15-federation/contract/fed.sh; source /course/modules/m18-frontier/contract/port/port.sh
W=$(mktemp -d)
sub_start reviews 4003 || exit 1
port_signatures http://localhost:4003/graphql > $W/sig.txt; cat $W/sig.txt
gql http://localhost:4003/graphql '{ _service { sdl } }' | jq -r .data._service.sdl > $W/reviews.graphqls
port_graph $W/reviews.graphqls || exit 1
hurl --test --continue-on-error --variable reviews=http://localhost:4003 --variable router=http://127.0.0.1:4000/ $PORT_DIR/port.hurl 2>&1 | tail -40

#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m13-testing/contract/mock/run-mock.sh
source /course/modules/m13-testing/contract/suite-test.sh
cd "$LAB_DIR" || exit 1
C=/course/modules/m13-testing/contract
SHELFIE=$C/shelfie.graphqls; SHOP=$LAB_HOME/tests/schemas/shop.graphqls; CP=$(lib_cp)

# Part A: your MockWiring behind the course's MockServer, on Shelfie's schema and on one it has never seen.
require_file MockWiring.java || finish
W=$(mktemp -d); SUITES+=("$W"); cp MockWiring.java $C/mock/MockServer.java "$W/"
mock_start 4000 java -cp "$CP" "$W/MockServer.java" $SHELFIE 4000 13 || finish
mock_start 4001 java -cp "$CP" "$W/MockServer.java" $SHOP 4001 13 || finish
mock_start 4002 java -cp "$CP" "$W/MockServer.java" $SHELFIE 4002 14 || finish
mock_check 4000 $SHELFIE $LAB_HOME/tests/queries/shelfie "Shelfie schema: shape and determinism"
mock_check 4000 $SHELFIE $LAB_HOME/tests/queries/shelfie "Shelfie schema: realistic values" --realistic
mock_check 4001 $SHOP $LAB_HOME/tests/queries/shop "a schema it has never seen (interfaces, union, custom scalars)" --realistic
q='{"query":"{ clubs { name members(first: 3) { name } } }"}'
a=$(curl -s -H 'Content-Type: application/json' -d "$q" localhost:4000/graphql); b=$(curl -s -H 'Content-Type: application/json' -d "$q" localhost:4002/graphql)
[[ -n "$a" && "$a" == *'"data"'* && "$a" != "$b" ]] && pass "another seed gives other data (seed 13 and 14 differ)" \
  || fail "another seed gives other data" "seed 13 and seed 14 answered the same: is the seed used?"

# Part B: the screen, built against the mock, run against the real Shelfie with zero changes.
if require_file ReviewsScreen.java; then
  out=$(SHELFIE_URL=http://localhost:4000 java -cp "$CP" ReviewsScreen.java b1 2>/dev/null)
  if grep -qE '^.+ by .+' <<<"$(sed -n 1p <<<"$out")" && grep -qE '^(Rated [0-9]\.[0-9]{2}|No ratings), latest [0-9]+ reviews?$' <<<"$(sed -n 2p <<<"$out")" \
     && (( $(grep -cE '^- [1-5]/5 @[^:]+: .+' <<<"$out") >= 1 )); then pass "the screen renders mock data in the agreed format"
  else fail "the screen renders mock data in the agreed format" "got: $(head -3 <<<"$out" | paste -sd'|')"; fi
  for b in b1 b5; do
    got=$(SHELFIE_URL=$REF java -cp "$CP" ReviewsScreen.java $b 2>/dev/null)
    [[ "$got" == "$(cat $LAB_HOME/tests/expected-$b.txt)" ]] && pass "the same screen on the real Shelfie: book $b" \
      || fail "the same screen on the real Shelfie: book $b" "$(diff <(echo "$got") $LAB_HOME/tests/expected-$b.txt | head -4 | paste -sd' ')"
  done
fi
mock_stop

# Part C: WireMock stands in for Shelfie downstream; your tests must catch five bugs in the digest client.
grep -rqE '^\s*@WireMockTest|^[^*/]*(WireMockExtension|new WireMockServer)' digest/src/test 2>/dev/null \
  && pass "the digest tests use WireMock" || fail "the digest tests use WireMock (digest/src/test)"
if suite_prepare $C/digest "$LAB_DIR/digest" && suite_baseline; then suite_mutants "$LAB_HOME/tests/digest-mutants.json"; fi
notes_check notes.md 60 m13/04
finish

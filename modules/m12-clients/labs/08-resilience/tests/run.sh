#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m12-clients/contract/clients-test.sh
cd "$LAB_DIR" || exit 1
ref_up || { fail "the reference server is not up: run bin/course ref up"; finish; }
curl -fsS -o /dev/null $TOXI_API/version || { fail "Toxiproxy is not reachable at $TOXI_API: run bin/course ref up"; finish; }
app_build target/shelfie-resilient.jar || finish
P=m12-resilience; FLAKY=http://toxiproxy:8667/graphql; T=$(mktemp -d)
trap 'toxi_clear $P; rm -rf $T' EXIT
# Toxiproxy decides per connection, so the client must not reuse connections for the faults to hit ~30% of calls.
run() { timeout 120 java -Djdk.httpclient.keepalive.timeout=0 -jar target/shelfie-resilient.jar "$@" 2>/dev/null; }
ok_pct() { sed -nE 's#^ok ([0-9]+)/([0-9]+).*#\1 \2#p' <<<"$1" | awk '{ if ($2) printf "%d", 100 * $1 / $2 }'; }
toxi_proxy $P 8667 || { fail "could not create the Toxiproxy proxy"; finish; }
toxi_toxic $P '{"name":"lost","type":"reset_peer","stream":"downstream","toxicity":0.3,"attributes":{"timeout":0}}'
toxi_toxic $P '{"name":"hang","type":"timeout","stream":"downstream","toxicity":0.05,"attributes":{"timeout":0}}'
plain=$(run $FLAKY load 200 --plain); resilient=$(run $FLAKY load 200)
note "plain:     ${plain:-no output (hung? a request without a timeout waits forever)}"
note "resilient: ${resilient:-no output}"
p=$(ok_pct "$plain"); r=$(ok_pct "$resilient")
[[ -n $p ]] && (( p <= 85 )) && pass "--plain: the faults are real (${p}% of 200 queries succeed)" \
  || fail "--plain: the faults are real (at most 85% of 200 queries succeed)" "got ${plain:-nothing}: --plain must be one attempt, no retry"
[[ -n $r ]] && (( r >= 97 )) && pass "resilient: at least 97% of 200 queries succeed (${r}%)" \
  || fail "resilient: at least 97% of 200 queries succeed" "got ${resilient:-nothing}"
# Mutations through lost responses: every review exactly once, and the client knows its id.
ref_reset_data
books=(b1 b2 b3 b4 b5 b6 b7 b8 b9 b10)
run $FLAKY reviews u11 "${books[@]}" > $T/reviews
curl -s -H 'Content-Type: application/json' -d '{"query":"{ user(id: \"u11\") { reviews { id book { id } } } }"}' $REF/graphql \
  | jq -r '.data.user.reviews[] | .book.id + " " + .id' | sort > $T/server
created=$(grep -cE '^b[0-9]+ r[0-9]+ (created|recovered)$' $T/reviews)
[[ $(wc -l < $T/server) == 10 && $(cut -d' ' -f1 $T/server | sort -u | wc -l) == 10 ]] \
  && pass "10 createReview calls through lost responses: Shelfie has exactly one review per book" \
  || fail "10 createReview calls through lost responses: Shelfie has exactly one review per book" "Shelfie has: $(paste -sd, $T/server)"
[[ $created == 10 && "$(cut -d' ' -f1,2 $T/reviews | sort)" == "$(cat $T/server)" ]] \
  && pass "the client reports all 10 as done, with the ids Shelfie stored ($(grep -c recovered $T/reviews) recovered after a lost response)" \
  || fail "the client reports all 10 as done, with the ids Shelfie stored" "client said: $(paste -sd, $T/reviews)"
ref_reset_data
# Shelfie unreachable: the circuit breaker must make 100 calls fail fast instead of retrying each one.
curl -fsS -o /dev/null -X POST $TOXI_API/proxies/$P -d '{"enabled":false}'
start=$(date +%s%N); down=$(run $FLAKY load 100); ms=$(( ($(date +%s%N) - start) / 1000000 ))
note "unreachable: ${down:-no output} (wall ${ms} ms)"
open=$(sed -nE 's#.*circuit open ([0-9]+).*#\1#p' <<<"$down")
[[ -n $open ]] && (( open >= 80 && ms < 15000 )) && pass "Shelfie unreachable: the breaker opens (${open} of 100 calls refused at once, ${ms} ms)" \
  || fail "Shelfie unreachable: the breaker opens (80+ of 100 calls refused at once, under 15 s)" "got ${down:-nothing} in ${ms} ms"
toxi_clear $P
notes_check notes.md 60 m12/08
finish

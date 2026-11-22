#!/usr/bin/env bash
# Lab 16.5: 100 club pages a second for 12 s (open model). At 4 s the reviews service turns slow (2 s per call), at
# 8 s it is killed. The page must keep rendering, fast, with REVIEWS_UNAVAILABLE on the reviews fields; then recover.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m16-production/contract/ops.sh
source /course/modules/m16-production/contract/lab-app.sh
cd "$LAB_DIR" || exit 1
trap 'app_stop; ops_stop' EXIT
lab_app_up --management.tracing.enabled=false || finish
PAGE=$(cat "$OPS/club-page.json")
r=$(gql_post "$PAGE")
[[ $(jq '.data.club.readingList | map(.reviews | length) | add' <<<"$r") -gt 0 && $(jq '.errors // [] | length' <<<"$r") == 0 ]] \
  && pass "the club page with reviews, normally" || fail "the club page with reviews" "$(cut -c1-200 <<<"$r")"
java -cp "$LIB/*" "$OPS/Peak.java" --seconds 3 --rate 50 >/dev/null 2>&1
( sleep 4; reviews_fault slow 2000; c4=$(curl -s localhost:4100/_stats | jq .reviewCalls); sleep 4
  c8=$(curl -s localhost:4100/_stats | jq .reviewCalls); echo $(( c8 - c4 )) >/tmp/slow-calls; kill $REVIEWS_PID 2>/dev/null ) & F=$!
P=$(java -cp "$LIB/*" "$OPS/Peak.java" --seconds 12 --rate 100 2>&1 | tail -1)
wait $F; REVIEWS_PID=
note "peak: $P"
jq -e .requests <<<"$P" >/dev/null 2>&1 || { fail "the load ran" "$P"; finish; }
[[ $(jq .failed <<<"$P") == 0 ]] && pass "every page rendered (1,200 club pages, reviews slow then dead)" \
  || fail "$(jq .failed <<<"$P") pages failed: no 200, a timeout (5 s), or no club name"
(( $(jq .p95 <<<"$P") < 250 )) && pass "p95 under 250 ms ($(jq .p95 <<<"$P") ms)" || fail "p95 $(jq .p95 <<<"$P") ms (target: under 250)" "per second: $(jq -c .p95PerSecond <<<"$P")"
(( $(jq .p99 <<<"$P") < 1000 )) && pass "p99 under 1 s ($(jq .p99 <<<"$P") ms)" || fail "p99 $(jq .p99 <<<"$P") ms (target: under 1000)"
calls=$(cat /tmp/slow-calls 2>/dev/null || echo '?')
[[ $calls =~ ^[0-9]+$ ]] && (( calls < 100 )) && pass "the slow service was spared: $calls calls in its 4 slow seconds (400 pages asked)" \
  || fail "the slow reviews service got $calls calls in 4 s: stop calling a dependency that is failing (a circuit breaker)"
(( $(jq .degraded <<<"$P") > 0 )) && pass "degraded answers carry REVIEWS_UNAVAILABLE ($(jq .degraded <<<"$P"))" || fail "no answer carried extensions.code REVIEWS_UNAVAILABLE"
r=$(gql_post "$PAGE")
jq -e '.data.club.name and (.data.club.readingList | length) == 4 and .data.club.currentBook.title' <<<"$r" >/dev/null \
  && jq -e '[.errors[] | select(.extensions.code == "REVIEWS_UNAVAILABLE" and .path[-1] == "reviews")] | length > 0' <<<"$r" >/dev/null \
  && jq -e '[.data.club.readingList[].reviews] | all(. == null)' <<<"$r" >/dev/null \
  && pass "while it is down: the page renders, reviews are null with REVIEWS_UNAVAILABLE at their path" \
  || fail "while the reviews service is down: club, books and titles present; reviews null; errors with extensions.code REVIEWS_UNAVAILABLE and the reviews path" "$(cut -c1-300 <<<"$r")"
reviews_start || finish
# A just-started service answers its first calls slowly (cold JVM); warm it so the breaker's half-open probes
# measure the service, not its start-up.
for _ in $(seq 1 20); do curl -s -o /dev/null "localhost:4100/reviews?books=b1"; done
ok=; for _ in $(seq 1 40); do r=$(gql_post "$PAGE"); [[ $(jq '.errors // [] | length' <<<"$r") == 0 ]] && { ok=1; break; }; sleep 0.5; done
[[ -n $ok ]] && pass "recovers once the reviews service is back (within 20 s)" || fail "still failing 20 s after the reviews service came back" "$(jq -c '.errors[0]' <<<"$r")"
finish

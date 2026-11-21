#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Timing.java || finish
cd "$LAB_DIR"
Q="$LAB_HOME/tests/queries/club-pages.graphql"
run() { timeout 60 java -cp "$(lib_cp)" Timing.java "$@" 2>/tmp/err.txt; }
run report "$Q" >/tmp/report.txt
got=$(grep -E '^slow [A-Za-z]+\.[A-Za-z]+ [0-9]+ ms$' /tmp/report.txt | awk '{print $2}' | sort | tr '\n' ' ')
want="Book.averageRating Club.meetups Query.clubs "
[[ "$got" == "$want" ]] && pass "report: the three fields slower than 50 ms (fetch time, children excluded)" \
  || fail "report should list exactly $want(one 'slow <Type.field> <ms> ms' line each)" "got: ${got:-nothing} $(tail -1 /tmp/err.txt)"
grep -qx "slowest Book.averageRating" /tmp/report.txt && pass "report: slowest is Book.averageRating" || fail "report should end with 'slowest Book.averageRating'" "got: $(grep slowest /tmp/report.txt)"
grep -qx "fetches 68" /tmp/report.txt && pass "report: 68 fetches counted" || fail "report should count every fetcher call: 'fetches 68'" "got: $(grep fetches /tmp/report.txt)"
run run "$Q" >/tmp/run.json
if jq -e '(.extensions.timing.slowFields | sort) == ["Book.averageRating","Club.meetups","Query.clubs"] and .extensions.timing.fetches == 68 and (.data.clubs | length) == 3' /tmp/run.json >/dev/null 2>&1
then pass "run: the timings travel in extensions.timing"; else fail "run should add extensions.timing {fetches, slowFields} to the spec map" "got: $(jq -c .extensions /tmp/run.json 2>/dev/null | cut -c1-150)"; fi
grep -q "ChainedInstrumentation" Timing.java && pass "instrumentations are chained" || fail "use ChainedInstrumentation to run Latency and your instrumentation together"
notes_check notes.md 60 m05/05
finish

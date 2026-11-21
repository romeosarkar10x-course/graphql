#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Bench.java || finish
require_file bakeoff.md || finish
APP=/course/modules/m05-graphql-java/labs/09-project-tiny-server/solution
(cd "$APP" && mvn -q package -DskipTests >/tmp/bench-build.log 2>&1) || { fail "could not build the reference app to benchmark" "$(tail -3 /tmp/bench-build.log)"; finish; }
cd "$LAB_DIR"
row=$(timeout 300 java Bench.java probe "$APP/src" "$APP" -- java -jar target/shelfie-server.jar 4000 2>/tmp/bench.err | tail -1)
IFS='|' read -r _ name lines startup rss p50 p95 rps _ <<<"$row"
num() { [[ "$(echo $1)" =~ ^[0-9]+(\.[0-9]+)?$ ]] && awk -v v="$1" 'BEGIN { exit !(v > 0) }'; }
num "$startup" && num "$rps" && pass "Bench runs: startup $(echo $startup) ms, $(echo $rps) req/s" || fail "Bench did not produce a row" "$(tail -2 /tmp/bench.err) $row"
num "$p95" && awk -v a="$p50" -v b="$p95" 'BEGIN { exit !(b >= a) }' && pass "percentiles: p50 $(echo $p50) ms <= p95 $(echo $p95) ms" || fail "implement percentile(): p50 and p95 must be positive, p95 >= p50" "got p50 '$p50' p95 '$p95'"
num "$rss" && (( $(echo $rss) > 20 )) && pass "RSS read from /proc: $(echo $rss) MB" || fail "implement rssMb(): read VmRSS from /proc/<pid>/status" "got '$rss'"
filled=$(grep -E '^\| (raw graphql-java|Spring for GraphQL|DGS|Quarkus) \|' bakeoff.md | grep -cE '^\|[^|]+(\| *[0-9][0-9.,]* *){6}\|$')
(( filled == 4 )) && pass "bakeoff.md: all four rows measured" || fail "bakeoff.md: fill all four rows with numbers ($filled of 4 so far)"
notes_check bakeoff.md 80 m06/09
finish

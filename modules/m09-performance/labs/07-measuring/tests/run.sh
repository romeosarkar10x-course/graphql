#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
# 1. JMH: the document cache must be measurably faster.
if (cd jmh && mvn -q package >/tmp/jmh-build.log 2>&1) && [[ -f jmh/target/benchmarks.jar ]]; then
  pass "jmh/ builds target/benchmarks.jar"
  if timeout 180 java -jar jmh/target/benchmarks.jar -wi 1 -i 2 -w 1 -r 1 -f 1 -rf json -rff /tmp/jmh.json >/tmp/jmh.log 2>&1; then
    parse=$(jq '[.[] | select(.benchmark | endswith("parseEveryTime")) | .primaryMetric.score][0] // 0' /tmp/jmh.json)
    cached=$(jq '[.[] | select(.benchmark | endswith("cachedDocument")) | .primaryMetric.score][0] // 0' /tmp/jmh.json)
    if awk -v p="$parse" -v c="$cached" 'BEGIN { exit !(c > 0 && p > 2 * c) }'; then pass "a cached document is at least 2x faster (parse $parse us, cached $cached us)"
    else fail "a cached document is at least 2x faster" "parseEveryTime $parse us, cachedDocument $cached us"; fi
  else fail "the benchmarks ran" "$(grep -m2 -E 'Exception|Error' /tmp/jmh.log)"; fi
else fail "jmh/ builds target/benchmarks.jar" "$(grep -m3 ERROR /tmp/jmh-build.log)"; fi
# 2. The simulation is an open workload with assertions.
sim=$(cat load/src/test/java/dev/shelfie/load/*.java 2>/dev/null)
grep -q 'injectOpen' <<<"$sim" && grep -q 'assertions' <<<"$sim" \
  && pass "DashboardSimulation: open workload with assertions" || fail "DashboardSimulation: open workload with assertions" "use injectOpen(constantUsersPerSec(...)) and .assertions(...)"
# 3. One measured run with the harness's copy of measure.sh: Gatling, JFR, async-profiler, Micrometer.
rm -rf /tmp/m07; HERE="$LAB_DIR" RESULTS=/tmp/m07 DURATION=10 timeout 400 bash "$LAB_HOME/tests/measure.sh" check >/tmp/measure.log 2>&1 || true
if grep -qE 'response time 95th percentile' /tmp/m07/check-gatling.txt 2>/dev/null && grep -qE '> KO +0 ' /tmp/m07/check-gatling.txt
then pass "Gatling ran the dashboard with no failures ($(grep -oE '95th percentile \(ms\) +\| +[0-9]+' /tmp/m07/check-gatling.txt | grep -oE '[0-9]+$') ms p95)"
else fail "Gatling ran the dashboard with no failures" "$(tail -3 /tmp/measure.log)"; fi
[[ -s /tmp/m07/check.jfr ]] && jfr summary /tmp/m07/check.jfr >/dev/null 2>&1 && pass "a JFR recording was written" || fail "a JFR recording was written"
[[ $(stat -c %s /tmp/m07/check-flame.html 2>/dev/null || echo 0) -gt 50000 ]] && pass "async-profiler wrote a flame graph" || fail "async-profiler wrote a flame graph"
grep -qE '^p0.95 [0-9.]+$' /tmp/m07/check-metrics.txt 2>/dev/null && pass "Micrometer reports graphql.request percentiles" \
  || fail "Micrometer reports graphql.request percentiles" "actuator, the metrics endpoint, and management.metrics.distribution.percentiles (README)"
# 4. "After" is the batched app.
app_start java -jar app/target/shelfie-perf.jar || finish
n=$(jq -n --rawfile q /course/modules/m09-performance/contract/dashboard.graphql '{query: $q}' | curl -s -m 10 -H 'Content-Type: application/json' -d @- "$URL" | jq '.extensions.sql.statements // 9999')
(( n <= 9 )) && pass "app/ is the batched version ($n statements per dashboard)" || fail "app/ is the batched version" "$n statements per dashboard; bring in your loaders from lab 9.3 after measuring 'before'"
app_stop
rm -rf /tmp/m07
# 5. The report: real numbers in both tables.
nums=$(grep -E '^\| (Before|After)' report.md 2>/dev/null | grep -oE '[0-9][0-9.,]* ?ms' | wc -l)
(( nums >= 10 )) && pass "report.md has before and after percentiles ($nums values)" || fail "report.md has before and after percentiles" "$nums values in ms; fill both tables"
notes_check report.md 150 m09/07
finish

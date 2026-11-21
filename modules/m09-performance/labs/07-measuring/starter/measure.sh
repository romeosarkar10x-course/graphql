#!/usr/bin/env bash
# Lab 9.7: one measured run of the app under load. Run it inside the course image:
#   bin/course shell -c 'cd workspace/m09-performance/07-measuring && ./measure.sh before'
# Builds app/, loads the data, starts the app with JDK Flight Recorder and async-profiler attached,
# warms it up for 10 s, then runs Gatling (load/) for 20 s at 20 dashboards per second. Writes:
#   results/<label>-gatling.txt   Gatling's summary (p50, p95, p99)
#   results/<label>.jfr           the flight recording; try: jfr view hot-methods results/<label>.jfr
#   results/<label>-flame.html    async-profiler's CPU flame graph (open it in a browser)
#   results/<label>-metrics.txt   Micrometer's graphql.request p50/p95/p99, from /actuator/metrics
# RESULTS=<dir> writes somewhere else, HERE=<dir> measures another copy of the lab; SCHEMA, RATE and DURATION change the data schema and the load.
set -euo pipefail
label="${1:?usage: measure.sh <label> [extra JVM options...]}"; shift
jvm_opts=("$@")
here="${HERE:-$(cd "$(dirname "$0")" && pwd)}"; out="${RESULTS:-$here/results}"; mkdir -p "$out"
source /course/tools/testlib.sh
java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java "${SCHEMA:-m09_07}" >/dev/null
(cd "$here/app" && mvn -q package -DskipTests)
# async-profiler 4.6 from Maven Central: its jar carries the native library for each platform.
ap=/tmp/async-profiler-4.6; arch=$(uname -m | sed 's/x86_64/x64/; s/aarch64/arm64/')
if [[ ! -f $ap/libasyncProfiler.so ]]; then
  mkdir -p $ap && (cd "$here/app" && mvn -q dependency:copy -Dartifact=tools.profiler:async-profiler:4.6 -DoutputDirectory=$ap)
  unzip -q -o -j $ap/async-profiler-4.6.jar "linux-$arch/libasyncProfiler.so" -d $ap
fi
# itimer: CPU sampling without perf_events, which containers usually forbid.
java "${jvm_opts[@]}" \
  -XX:StartFlightRecording=filename="$out/$label.jfr",settings=profile \
  -agentpath:$ap/libasyncProfiler.so=start,event=itimer,interval=1ms,file="$out/$label-flame.html" \
  -jar "$here/app/target/shelfie-perf.jar" > "$out/$label-app.log" 2>&1 &
pid=$!; trap 'kill $pid 2>/dev/null; wait $pid 2>/dev/null || true' EXIT
for _ in $(seq 120); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
cd "$here/load"
mvn -q gatling:test -Dgatling.simulationClass=dev.shelfie.load.DashboardSimulation -Drate=20 -Dseconds=10 >/dev/null 2>&1 || true   # warm-up
mvn -q gatling:test -Dgatling.simulationClass=dev.shelfie.load.DashboardSimulation -Drate="${RATE:-20}" -Dseconds="${DURATION:-20}" \
  | sed -n '/Global Information/,/^=====/p' | tee "$out/$label-gatling.txt"
for phi in 0.5 0.95 0.99; do
  printf 'p%s %s\n' "$phi" "$(curl -s "localhost:4000/actuator/metrics/graphql.request.percentile?tag=phi:$phi" | jq -r '.measurements[0].value // "none"' 2>/dev/null || echo none)"
done > "$out/$label-metrics.txt"
kill $pid; wait $pid 2>/dev/null || true; trap - EXIT
echo "results in $out/$label-*"

#!/usr/bin/env bash
# Probe 8: lab 16.5's experiment. 100 club pages a second for 12 s (open model); reviews slow (2 s) at 4 s, killed at 8 s.
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
C=/course/modules/m16-production/contract
run() {
  reviews_start || return 1
  java -jar $1 --management.tracing.enabled=false --logging.level.root=warn > /tmp/app.log 2>&1 & APP=$!
  for i in $(seq 60); do curl -fs -o /dev/null localhost:4000/actuator/health && break; sleep 0.5; done
  java -cp "$LIB/*" $C/Peak.java --seconds 4 --rate 50 >/dev/null   # warm up
  curl -s -X POST localhost:4100/_stats/reset >/dev/null
  ( sleep 4; reviews_fault slow 2000; sleep 4; kill $REVIEWS_PID ) & F=$!
  java -cp "$LIB/*" $C/Peak.java --seconds 12 --rate 100
  wait $F; curl -s localhost:4100/_stats 2>/dev/null; kill $APP; wait $APP 2>/dev/null; REVIEWS_PID=
}
cd /course/_workbench/2026-11-22-m16-build/fragile-app && mvn -q package -DskipTests || exit 1
cd /course/modules/m16-production/contract/ops-app && mvn -q package -DskipTests || exit 1
echo "== fragile (no timeouts, no breaker)"; run /course/_workbench/2026-11-22-m16-build/fragile-app/target/shelfie-ops.jar
echo "== resilient (bulkhead, breaker, 500 ms timeout)"; run $C/ops-app/target/shelfie-ops.jar

#!/usr/bin/env bash
# Lab 11.5 experiment: 500 clients polling every 2 s vs 500 subscribers, 20 s, 1 message a second (CDC solution).
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd /course/modules/m11-realtime/labs/05-cdc/solution && mvn -q package -DskipTests
app_start java -Xmx384m -jar target/shelfie-live.jar
for _ in $(seq 1 60); do grep -q 'change feed: streaming' /tmp/app.log && break; sleep 0.5; done
grep -E 'Started LiveApplication|change feed' /tmp/app.log | cut -c1-160
for mode in idle poll subscribe; do
  java -cp "$(lib_cp)" /course/modules/m11-realtime/contract/Load.java $mode --clients 500 --seconds 20 --pid $APP_PID
  sleep 3
done
grep -cE 'ERROR' /tmp/app.log

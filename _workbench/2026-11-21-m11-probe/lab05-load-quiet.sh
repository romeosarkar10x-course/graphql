#!/usr/bin/env bash
# Lab 11.5 experiment, quiet room: one message every 10 s, 30 s, 500 clients.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd /course/modules/m11-realtime/labs/05-cdc/solution
app_start java -Xmx384m -jar target/shelfie-live.jar
for _ in $(seq 1 60); do grep -q 'change feed: streaming' /tmp/app.log && break; sleep 0.5; done
for mode in idle poll subscribe; do
  java -cp "$(lib_cp)" /course/modules/m11-realtime/contract/Load.java $mode --clients 500 --seconds 30 --send-every 10000 --pid $APP_PID
  sleep 3
done

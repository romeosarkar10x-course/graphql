#!/usr/bin/env bash
# Lab 11.3 experiment: 10,000 typing events/s for 3 s from 3 bots, a fast and a slow (20 frames/s) client.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
for v in starter solution; do
  cd /course/modules/m11-realtime/labs/03-implementing/$v; mvn -q package -DskipTests
  app_start java -Xmx256m -jar target/shelfie-live.jar
  D='subscription($r: ID!, $me: String) { typing(roomId: $r, me: $me) { author } }'
  ws_bg /tmp/fast "$D" --vars '{"r":"r2","me":"Ada"}' --seconds 12
  ws_bg /tmp/slow "$D" --vars '{"r":"r2","me":"Ben"}' --seconds 12 --read-rate 20
  curl -s -X POST "localhost:4000/_course/flood?roomId=r2&perSecond=10000&seconds=3&authors=3"; echo
  for s in 1 2 3 4 6 8 10 12; do sleep $([[ $s -le 4 ]] && echo 1 || echo 2)
    echo "$v t=${s}s fast=$(grep -c ' next ' /tmp/fast) slow=$(grep -c ' next ' /tmp/slow) rss=$(( $(awk '/VmRSS/{print $2}' /proc/$APP_PID/status) / 1024 ))MB cpu=$(awk '{print $14+$15}' /proc/$APP_PID/stat)ticks"
  done
  echo "$v fast first/last: $(grep ' next ' /tmp/fast | head -1 | cut -c1-60) / $(grep ' next ' /tmp/fast | tail -1 | cut -c1-60)"
  echo "$v slow last: $(grep ' next ' /tmp/slow | tail -1 | cut -c1-60)"
  ws_stop; app_stop
done

#!/usr/bin/env bash
# Lab 11.2 reference run: the same subscription over both transports, and the client across a drop.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd /course/modules/m11-realtime/labs/02-transports/solution
app_start java -jar target/shelfie-live.jar
D='subscription { messageSent(roomId: "r1") { id author body } }'
echo "== WebSocket (Ws.java prints the payload of each frame)"
ws_bg /tmp/w "$D" --seconds 7
echo "== SSE (raw bytes, cat -A)"
(curl -sN -m 7 -H 'Content-Type: application/json' -H 'Accept: text/event-stream' -d "$(jq -nc --arg q "$D" '{query:$q}')" "$URL" -D /tmp/h > /tmp/s &)
sleep 0.5; send r1 Ada "one" >/dev/null; sleep 0.3; send r1 Ben "two" >/dev/null; sleep 6.5
cat /tmp/w; echo "-- SSE headers"; cat /tmp/h; echo "-- SSE body"; cat -A /tmp/s
echo "== client across a 1.5 s drop (stderr then stdout)"
balancer 4100 4000
for t in ws sse; do
  java -cp "$(lib_cp)" client/Chat.java http://localhost:4100/graphql r3 --transport $t --seconds 6 > /tmp/o 2> /tmp/e & C=$!
  sleep 1; send r3 Ada "before" >/dev/null; sleep 0.3; echo "drop 1500" > /tmp/balancer-4100; sleep 0.3
  send r3 Ben "during" >/dev/null; sleep 3; send r3 Cleo "after" >/dev/null; wait $C
  echo "-- $t stderr"; cat /tmp/e; echo "-- $t stdout"; cat /tmp/o
done
kill $BAL_PID

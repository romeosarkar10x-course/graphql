#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-live.jar || finish
app_start java -Xmx256m -jar target/shelfie-live.jar || finish
trap 'ws_stop; app_stop' EXIT
T=$(mktemp -d)
typing_doc='subscription($r: ID!, $me: String) { typing(roomId: $r, me: $me) { author } }'
watching() { gql "{\"query\":\"{ room(id: \\\"$1\\\") { watching } }\"}" | jq -r '.data.room.watching'; }

# 1. Ada's own typing is not echoed to her; Ben's is.
ws_bg $T/ada "$typing_doc" --vars '{"r":"r1","me":"Ada"}' --seconds 3; sleep 0.3
for who in Ada Ben Ada; do gql "{\"query\":\"mutation { typing(roomId: \\\"r1\\\", author: \\\"$who\\\") }\"}" >/dev/null; sleep 0.3; done
sleep 0.5; seen=$(nexts $T/ada | jq -r .data.typing.author | sort -u | paste -sd,)
[[ "$seen" == Ben ]] && pass "typing(me: \"Ada\"): Ada sees Ben typing, never herself" \
  || fail "typing(me: \"Ada\"): Ada sees Ben typing, never herself" "Ada saw: ${seen:-nothing}"
ws_stop

# 2. A 3 s flood of 10,000 typing events a second from 3 bots, watched by a fast and a slow client.
ws_bg $T/fast "$typing_doc" --vars '{"r":"r2","me":"Ada"}' --seconds 7
ws_bg $T/slow "$typing_doc" --vars '{"r":"r2","me":"Ben"}' --seconds 7 --read-rate 20
curl -s -X POST "http://localhost:$PORT/_course/flood?roomId=r2&perSecond=10000&seconds=3&authors=3" >/dev/null
sleep 7.5
fast=$(grep -c ' next ' $T/fast); late=$(awk '$2 == "next" && $1 > 4800' $T/slow | wc -l)
(( fast >= 10 && fast <= 60 )) \
  && pass "flood: the fast client gets a few events per author per 250 ms, not 30,000 ($fast)" \
  || fail "flood: the fast client gets a few events per author per 250 ms, not 30,000" "it got $fast (want 10 to 60)"
(( late == 0 )) \
  && pass "flood: a slow client (20 frames/s) is not still reading stale events 1.5 s after the flood" \
  || fail "flood: a slow client (20 frames/s) is not still reading stale events 1.5 s after the flood" "$late events arrived after 4.8 s"
kill -0 $APP_PID 2>/dev/null && pass "flood: the server survived" || fail "flood: the server survived" "$(grep -m1 -E 'Error|Exception' /tmp/app.log)"
ws_stop

# 3. Cleanup: three subscriptions on r3 are counted while open and released when the clients vanish.
ws_bg $T/w1 'subscription { messageSent(roomId: "r3") { id } }' --seconds 30
ws_bg $T/w2 'subscription { messageSent(roomId: "r3") { id } }' --seconds 30
ws_bg $T/w3 'subscription { typing(roomId: "r3") { author } }' --seconds 30
sleep 0.5; open=$(watching r3); ws_stop; sleep 1.5; after=$(watching r3)
[[ "$open" == 3 && "$after" == 0 ]] \
  && pass "cleanup: room r3 counts 3 open subscriptions, then 0 after the clients disconnect" \
  || fail "cleanup: room r3 counts 3 open subscriptions, then 0 after the clients disconnect" "while open: $open, after: $after"

# 4. takeUntil: archiving a room completes its subscriptions.
ws_bg $T/arch 'subscription { messageSent(roomId: "r1") { id } }' --seconds 5; sleep 0.3
gql '{"query":"mutation { archiveRoom(id: \"r1\") { archived } }"}' >/dev/null; sleep 1
grep -q ' complete' $T/arch && [[ "$(watching r1)" == 0 ]] \
  && pass "archiveRoom(r1): its open subscriptions complete and the room counts 0" \
  || fail "archiveRoom(r1): its open subscriptions complete and the room counts 0" "frames: $(cut -c1-40 $T/arch | paste -sd'|'); watching: $(watching r1)"
ws_stop
app_stop
notes_check notes.md 60 m11/03
finish

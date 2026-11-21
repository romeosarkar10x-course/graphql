#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-live.jar || finish
app_start java -jar target/shelfie-live.jar || finish
trap 'ws_stop; [[ -n "$CHAT" ]] && kill $CHAT 2>/dev/null; [[ -n "$BAL_PID" ]] && kill $BAL_PID; app_stop' EXIT
T=$(mktemp -d)
# Keep-alive: an idle subscription must hear from the server within 7 s on both transports.
ws_bg $T/idle 'subscription { messageSent(roomId: "r3") { id } }' --seconds 7
curl -sN -m 7 -H 'Content-Type: application/json' -H 'Accept: text/event-stream' \
  -d '{"query":"subscription { messageSent(roomId: \"r3\") { id } }"}' "$URL" > $T/sse-idle 2>/dev/null
grep -q ' ping' $T/idle && pass "WebSocket keep-alive: a ping within 7 s on an idle subscription" \
  || fail "WebSocket keep-alive: a ping within 7 s on an idle subscription" "got: $(cut -c1-60 $T/idle | paste -sd'|')"
grep -q '^:' $T/sse-idle && pass "SSE keep-alive: a comment line within 7 s on an idle stream" \
  || fail "SSE keep-alive: a comment line within 7 s on an idle stream" "got $(wc -c < $T/sse-idle) bytes"
ws_stop
# The client connects through a balancer that drops every connection for 1.5 s, like a network blip.
balancer 4100 $PORT || { fail "the test's balancer did not start"; finish; }
for transport in ws sse; do
  room=r1; [[ $transport == sse ]] && room=r2
  java -cp "$(lib_cp)" client/Chat.java http://localhost:4100/graphql $room --transport $transport --seconds 14 > $T/$transport.out 2> $T/$transport.err & CHAT=$!
  for _ in $(seq 1 50); do grep -q '^connected' $T/$transport.err && break; sleep 0.1; done
  sleep 0.5
  sent=()
  for i in 1 2 3; do sent+=("$(send $room Ada "before $i")"); done
  sleep 0.5
  echo "drop 1500" > /tmp/balancer-4100; sleep 0.3
  for i in 4 5 6; do sent+=("$(send $room Ben "during $i")"); done   # sent while the client is cut off
  sleep 3
  for i in 7 8 9; do sent+=("$(send $room Cleo "after $i")"); done
  sleep 1.5; kill $CHAT 2>/dev/null; wait $CHAT 2>/dev/null; CHAT=
  want=$(printf '%s\n' "${sent[@]}" | paste -sd,); have=$(cut -d' ' -f1 $T/$transport.out | paste -sd,)
  grep -q '^dropped' $T/$transport.err || { fail "$transport: the client noticed the drop" "stderr: $(head -c 300 $T/$transport.err)"; continue; }
  [[ "$have" == "$want" ]] \
    && pass "$transport: all 9 messages exactly once and in order across a dropped connection" \
    || fail "$transport: all 9 messages exactly once and in order across a dropped connection" "want $want" "have ${have:-nothing}"
done
app_stop
notes_check notes.md 60 m11/02
finish

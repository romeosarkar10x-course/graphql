#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-live.jar || finish
app_start java -jar target/shelfie-live.jar || finish
trap 'ws_stop; app_stop' EXIT
T=$(mktemp -d)
doc='subscription($r: ID!) { messageSent(roomId: $r) { id roomId author body } }'
ws_bg $T/a "$doc" --vars '{"r":"r1"}' --seconds 8; ws_bg $T/b "$doc" --vars '{"r":"r1"}' --seconds 8
ws_bg $T/c "$doc" --vars '{"r":"r2"}' --seconds 8
sleep 0.3
send r1 Ada "hello, live chat" >/dev/null; send r2 Ben "another room" >/dev/null
ws_wait $T/a 1 3; ws_wait $T/b 1 3; ws_wait $T/c 1 3; sleep 0.5
got() { nexts "$1" | jq -r '.data.messageSent | "\(.roomId) \(.author): \(.body)"' 2>/dev/null | paste -sd'|'; }
[[ "$(got $T/a)" == "r1 Ada: hello, live chat" && "$(got $T/b)" == "r1 Ada: hello, live chat" ]] \
  && pass "messageSent: both subscribers to r1 get the one message sent there" \
  || fail "messageSent: both subscribers to r1 get the one message sent there" "first got: $(got $T/a); second got: $(got $T/b)"
[[ "$(got $T/c)" == "r2 Ben: another room" ]] \
  && pass "messageSent: a subscriber to r2 gets only r2's message" \
  || fail "messageSent: a subscriber to r2 gets only r2's message" "got: $(got $T/c)"
ws_stop
ws_bg $T/d 'subscription { messageSent(roomId: "nope") { id } }' --seconds 3; sleep 1
grep -q ' error .*no room nope' $T/d && grep -q 'NOT_FOUND' $T/d \
  && pass "messageSent on an unknown room: a graphql-transport-ws error, NOT_FOUND" \
  || fail "messageSent on an unknown room: a graphql-transport-ws error, NOT_FOUND" "got: $(tail -2 $T/d | cut -c1-200)"
ws_bg $T/e 'subscription { countdown(from: 3) }' --seconds 3; sleep 0.8
[[ "$(nexts $T/e | jq -r .data.countdown | paste -sd,)" == "3,2,1" ]] && grep -q ' complete' $T/e \
  && pass "countdown(from: 3): next 3, 2, 1, then complete" \
  || fail "countdown(from: 3): next 3, 2, 1, then complete" "got: $(cut -c1-80 $T/e | paste -sd'|')"
ws_bg $T/f 'subscription { countdown(from: 5, failAt: 2) }' --seconds 3; sleep 0.8
err=$(grep ' error ' $T/f | sed 's/^ *[0-9]* error //')
[[ "$(nexts $T/f | jq -r .data.countdown | paste -sd,)" == "5,4,3" ]] \
  && [[ "$(jq -r '.[0].extensions.code' <<<"$err" 2>/dev/null)" == SOURCE_STREAM_FAILED ]] \
  && jq -e '.[0].message | test("failAt")' <<<"$err" >/dev/null 2>&1 \
  && pass "a failing source stream: 5, 4, 3, then an error naming the cause, code SOURCE_STREAM_FAILED" \
  || fail "a failing source stream: 5, 4, 3, then an error naming the cause, code SOURCE_STREAM_FAILED" "got: $(cut -c1-160 $T/f | paste -sd'|')"
app_stop
notes_check notes.md 60 m11/01
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd "$LAB_DIR" || exit 1
sql() { java -cp "$(lib_cp)" $LIVE/Sql.java "$1"; }
app_build target/shelfie-live.jar || finish
app_start java -Xmx384m -jar target/shelfie-live.jar || finish
trap 'ws_stop; app_stop' EXIT
for _ in $(seq 1 60); do grep -q 'change feed: streaming' /tmp/app.log && break; sleep 0.5; done   # the solution logs this; harmless otherwise
T=$(mktemp -d)
ws_bg $T/r1 'subscription { messageSent(roomId: "r1") { id author body } }' --seconds 20; sleep 0.5
id=$(send r1 Ada "through the API")
sleep 2.5
n=$(nexts $T/r1 | jq -r 'select(.data.messageSent.body == "through the API") | .data.messageSent.id' | wc -l)
(( n == 1 )) && pass "a message sent with sendMessage reaches the subscriber exactly once" \
  || fail "a message sent with sendMessage reaches the subscriber exactly once" "it arrived $n times"
sql "INSERT INTO m11.messages (room_id, author, body) VALUES ('r1', 'batch-job', 'written straight to the table')" >/dev/null
sql "INSERT INTO m11.messages (room_id, author, body) VALUES ('r2', 'batch-job', 'another room')" >/dev/null
ws_wait $T/r1 2 5; sleep 0.5
got=$(nexts $T/r1 | jq -r '.data.messageSent | "\(.author): \(.body)"' | paste -sd'|')
[[ "$got" == *"batch-job: written straight to the table"* ]] \
  && pass "a row INSERTed by another program (not the API) reaches the subscriber" \
  || fail "a row INSERTed by another program (not the API) reaches the subscriber" "the r1 subscriber got: ${got:-nothing}"
[[ "$got" != *"another room"* ]] && pass "a row for room r2 does not reach r1's subscriber" \
  || fail "a row for room r2 does not reach r1's subscriber" "got: $got"
sql "UPDATE m11.messages SET body = 'edited' WHERE author = 'batch-job'" >/dev/null; sleep 1.5
[[ "$(grep -c ' next ' $T/r1)" == 2 ]] && pass "an UPDATE is not a new message" \
  || fail "an UPDATE is not a new message" "the subscriber got $(grep -c ' next ' $T/r1) events, want 2"
via=$(gql '{"query":"{ messages(roomId: \"r1\", last: 1) { author body } }"}' | jq -r '.data.messages[0] | "\(.author): \(.body)"')
[[ "$via" == "batch-job: edited" ]] && pass "Query.messages reads the same table" || fail "Query.messages reads the same table" "last r1 message: $via"
ws_stop; app_stop
notes_check notes.md 60 m11/05
finish

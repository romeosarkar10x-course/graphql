#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
T="java /course/modules/m10-security/contract/Token.java"
ada=$($T ada); ben=$($T ben); olu=$($T olu)
app_build target/shelfie-secure.jar || finish
app_start java -jar target/shelfie-secure.jar || finish
ws() { timeout 30 java "$LAB_HOME/tests/WsProbe.java" "ws://localhost:$PORT/graphql" "$@" 2>&1 | tail -1; }
check() { # label, expected prefix, outcome
  [[ "$3" == "$2"* ]] && pass "$1" || fail "$1" "got: ${3:-nothing}"; }
check "W01 a handshake from http://evil.test is refused (403)" "handshake-refused 403" "$(ws http://evil.test "$ada" c1 "$olu")"
check "W02 ada authenticates in connection_init and gets c1's new review live" "next live note" "$(ws https://shelfie.test "$ada" c1 "$olu")"
check "W03 no token: the subscription to private c1 is refused" "error" "$(ws https://shelfie.test - c1 "$olu")"
check "W04 a forged token in connection_init closes the socket" "closed 44" "$(ws https://shelfie.test "$($T ada --forged)" c1 "$olu")"
check "W05 ben (not a member) cannot subscribe to c1" "error" "$(ws https://shelfie.test "$ben" c1 "$olu")"
app_hurl "$LAB_HOME/tests/transport.hurl" "CORS and error messages (transport.hurl)"
curl -s -o /dev/null -H 'Content-Type: application/json' -H "Authorization: Bearer $ada" -H 'X-Api-Key: pk_reader_1' -d '{"query":"{ viewer { id } }"}' "$URL"
sleep 1
sig=${ada##*.}
if grep -qF "$sig" /tmp/app.log || grep -q 'pk_reader_1' /tmp/app.log; then fail "the log holds no credentials" "a bearer token or API key was written to the log"
elif grep -qE 'POST /graphql' /tmp/app.log; then pass "requests are logged, credentials redacted"
else fail "requests are still logged (method and path), with credentials redacted"; fi
grep -q 'sk_live_7f3a9c2e1b' /tmp/app.log && pass "the failing resolver's detail is in the server log, for the team" \
  || fail "the failing resolver's detail reaches the server log" "mask it for the client, not for yourself"
app_stop
notes_check notes.md 60 m10/07
finish

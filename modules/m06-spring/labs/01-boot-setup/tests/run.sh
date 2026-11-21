#!/usr/bin/env bash
source /course/tools/testlib.sh
source "$LAB_HOME/../../contract/app-test.sh"
cd "$LAB_DIR" || exit 1
app_build target/shelfie-boot.jar || finish
app_start java -jar target/shelfie-boot.jar || finish
grep -q "Started ShelfieApplication" /tmp/app.log && pass "Spring Boot $(grep -oE 'Spring Boot[^)]*v[0-9.]+' /tmp/app.log | head -1 | grep -oE '[0-9.]+$') started $(grep -oE "in [0-9.]+ seconds" /tmp/app.log | head -1)"
app_hurl "$LAB_HOME/../../contract/shelfie.hurl" "Shelfie on Spring (shelfie.hurl)"
code=$(curl -sL -o /dev/null -w "%{http_code}" "http://localhost:$PORT/graphiql")
[[ "$code" == 200 ]] && pass "GraphiQL is served at /graphiql" || fail "GraphiQL should be enabled at /graphiql" "got HTTP $code"
by=$(curl -s -H 'Content-Type: application/json' -d '{"query":"{ servedBy }"}' "$URL" | jq -r '.data.servedBy // empty')
[[ "$by" == virtual ]] && pass "requests run on virtual threads" || fail "requests should run on virtual threads" "servedBy: ${by:-no answer}"
app_stop
notes_check notes.md 60 m06/01
finish

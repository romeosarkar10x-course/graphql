#!/usr/bin/env bash
source /course/tools/testlib.sh
source "$LAB_HOME/../../contract/app-test.sh"
cd "$LAB_DIR" || exit 1
app_build target/shelfie-boot.jar || finish
app_start java -jar target/shelfie-boot.jar || finish
q='{ a: trouble(kind: \"NOT_FOUND\") b: trouble(kind: \"FORBIDDEN\") c: trouble(kind: \"INVALID\") d: trouble(kind: \"CONFLICT\") e: trouble(kind: \"TIMEOUT\") f: trouble(kind: \"BUG\") hello }'
curl -s -H 'Content-Type: application/json' -d "{\"query\":\"$q\"}" "$URL" >/tmp/six.json
curl -s -H 'Content-Type: application/json' -d '{"query":"{ club(id: \"c1\") { name trouble(kind: \"FORBIDDEN\") } }"}' "$URL" >/tmp/nested.json
source "$LAB_HOME/tests/check.sh"
app_hurl "$LAB_HOME/../../contract/shelfie.hurl" "Shelfie on Spring (shelfie.hurl)"
app_stop
grep -q "pool exhausted" /tmp/app.log && pass "the bug's details are in the server log" || fail "the real exception should be logged"
notes_check notes.md 60 m06/06
finish

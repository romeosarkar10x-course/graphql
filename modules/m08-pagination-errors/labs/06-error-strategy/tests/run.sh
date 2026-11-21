#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-errors.jar || finish
app_start java -jar target/shelfie-errors.jar || finish
app_hurl "$LAB_HOME/tests/lab.hurl" "error contract (lab.hurl)"
# The errorId a client sees must find the full exception in the log.
id=$(curl -s -H 'Content-Type: application/json' -d '{"query":"{ book(id: \"b7\") { coverUrl } }"}' "$URL" | jq -r '.errors[0].extensions.errorId // empty')
app_stop
if [[ -n "$id" ]] && grep -F "$id" /tmp/app.log >/dev/null && grep -q 'shelfie-covers-prod' /tmp/app.log; then
  pass "the errorId finds the logged exception, internals and all"
else fail "the errorId finds the logged exception, internals and all" "errorId '${id}' must appear in the log next to the original exception"; fi
notes_check error-contract.md 120 m08/06
finish

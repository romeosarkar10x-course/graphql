#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Errors.java || finish
cd "$LAB_DIR"
run() { timeout 60 java -cp "$(lib_cp)" Errors.java run "$LAB_HOME/tests/queries/$1.graphql" 2>/tmp/stderr.txt; }
run five-kinds >/tmp/five.json
jq -e . /tmp/five.json >/dev/null 2>&1 || { fail "run five-kinds printed no JSON" "$(tail -3 /tmp/stderr.txt)"; finish; }
err() { jq -c --arg p "$1" '[.errors[]? | select(.path == [$p])][0]' /tmp/five.json; }
expect() { # label jq-test path
  if jq -e --arg p "$3" "[.errors[]? | select(.path == [\$p])][0] | $2" /tmp/five.json >/dev/null 2>&1; then pass "$1"
  else fail "$1" "got: $(err "$3" | cut -c1-200)"; fi
}
expect "NotFoundException: classification NOT_FOUND, its own message" '.extensions.classification == "NOT_FOUND" and .message == "No such club zz"' a
expect "ForbiddenException: classification FORBIDDEN" '.extensions.classification == "FORBIDDEN" and .message == "Not allowed to read the notes of a private club"' b
expect "InvalidInputException: BAD_USER_INPUT with extensions.field" '.extensions.classification == "BAD_USER_INPUT" and .extensions.field == "rating"' c
expect "TimeoutException: UNAVAILABLE, retryable, no internal service name" '.extensions.classification == "UNAVAILABLE" and .extensions.retryable == true and (.message | test("ratings-service") | not)' d
expect "a bug: INTERNAL, no internals in the message, a ref to quote" '.extensions.classification == "INTERNAL" and (.extensions.ref | type == "string" and length > 0) and (.message | test("Null|jdbc|length|10\\.0\\.") | not)' e
ref=$(jq -r '[.errors[]? | select(.path == ["e"])][0].extensions.ref // empty' /tmp/five.json)
if [[ -n "$ref" ]] && jq -e --arg r "$ref" '[.errors[] | select(.path == ["e"])][0] | (.message | contains($r))' /tmp/five.json >/dev/null; then pass "the INTERNAL message quotes its ref"
else fail "the INTERNAL error's message should quote its extensions.ref"; fi
[[ -n "$ref" ]] && grep -q "$ref" /tmp/stderr.txt && grep -q "NullPointerException" /tmp/stderr.txt \
  && pass "the log (stderr) has the ref and the real exception" || fail "log the real exception with the same ref to stderr"
jq -e '(.errors | length) == 5 and all(.errors[]; (.locations | length) == 1) and .data.hello == "Hello from Shelfie" and .data.e == null' /tmp/five.json >/dev/null \
  && pass "five errors with locations; the other fields still resolve" || fail "expect exactly 5 errors, each with locations, and data.hello resolved"
run nested >/tmp/nested.json
jq -e '.data.club == null and .data.hello != null and ([.errors[].path] | sort) == [["club","mustWork"],["club","trouble"]]' /tmp/nested.json >/dev/null 2>&1 \
  && pass "nested: paths point into club; the non-null failure nulls club" || fail "nested: two errors at [club, trouble] and [club, mustWork], data.club null" "got: $(jq -c . /tmp/nested.json 2>/dev/null | cut -c1-200)"
notes_check notes.md 60 m05/06
finish

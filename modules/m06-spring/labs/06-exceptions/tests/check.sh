# Lab 6.6 checks on one response. Sourced by run.sh with /tmp/six.json and /tmp/nested.json in place.
err() { jq -c --arg p "$1" '[.errors[]? | select(.path == [$p])][0]' /tmp/six.json; }
expect() { # label jq-test alias
  if jq -e --arg p "$3" "[.errors[]? | select(.path == [\$p])][0] | $2" /tmp/six.json >/dev/null 2>&1; then pass "$1"
  else fail "$1" "got: $(err "$3" | cut -c1-220)"; fi
}
expect "NotFound: NOT_FOUND, its message" '.extensions.classification == "NOT_FOUND" and .message == "No such club zz"' a
expect "Forbidden: FORBIDDEN" '.extensions.classification == "FORBIDDEN" and .message == "Not allowed to read this club'"'"'s notes"' b
expect "Invalid: BAD_REQUEST with extensions.field" '.extensions.classification == "BAD_REQUEST" and .extensions.field == "rating"' c
expect "Conflict: extensions.code CONFLICT and currentVersion 7" '.extensions.code == "CONFLICT" and .extensions.currentVersion == 7' d
expect "TimeoutException: retryable, no internal service name" '.extensions.retryable == true and (.message | test("ratings-service") | not)' e
expect "a bug: INTERNAL_ERROR, nothing internal in the message" '.extensions.classification == "INTERNAL_ERROR" and (.message | test("jdbc|10\\.0\\.|pool|IllegalState") | not)' f
jq -e '(.errors | length) == 6 and all(.errors[]; (.locations | length) == 1) and .data.hello == "Hello from Shelfie"' /tmp/six.json >/dev/null \
  && pass "six errors, each with path and locations; hello still resolves" || fail "expect 6 errors with locations and data.hello"
jq -e '.data.club.name == "The Midnight Readers" and .data.club.trouble == null and .errors[0].path == ["club","trouble"] and .errors[0].extensions.classification == "FORBIDDEN"' /tmp/nested.json >/dev/null 2>&1 \
  && pass "nested: the handler sees the nested field's path" || fail "nested trouble should give a FORBIDDEN error at [club, trouble]" "got: $(cut -c1-200 /tmp/nested.json)"

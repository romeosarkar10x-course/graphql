#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file club-card.graphql || finish
doc="$LAB_DIR/club-card.graphql"
grep -qE '\$detailed *: *Boolean! *= *false' "$doc" && pass "declares \$detailed: Boolean! = false" \
  || fail "declare \$detailed: Boolean! = false"
grep -q '@include' "$doc" && grep -q '@skip' "$doc" && pass "uses both @include and @skip" || fail "use @include for the details and @skip for the teaser"

send() { jq -Rs --argjson v "$2" '{query: ., variables: $v}' "$1" \
  | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql" | jq -c .; }
check() { # label, variables, expected-file
  send "$doc" "$2" >/tmp/out.json
  if json_equal /tmp/out.json "$LAB_HOME/tests/expected/$3"; then pass "$1"
  else fail "$1: response differs (< yours, > expected)"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/$3"; fi
}
check "no \$detailed: compact card" '{"id":"c2"}' compact.json
check "\$detailed false: compact card" '{"id":"c2","detailed":false}' compact.json
check "\$detailed true: detailed card" '{"id":"c2","detailed":true}' detailed.json

sub="$LAB_DIR/subscription-skip.graphql"
if grep -qE '^\s*subscription' "$sub" 2>/dev/null && grep -qE 'meetupScheduled[^{]*@(skip|include)' "$sub"; then
  pass "subscription-skip.graphql puts a directive on the subscription root field"
else fail "subscription-skip.graphql: a subscription with @skip or @include on meetupScheduled itself"; fi

lines=$(grep -cvE '^\s*(#.*)?$' "$LAB_DIR/notes.md" 2>/dev/null || echo 0)
(( lines >= 9 )) && pass "notes.md filled in (rubric: bin/course grade m02/05)" || fail "notes.md: answer the questions"
finish

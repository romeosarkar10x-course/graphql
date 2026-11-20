#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file club-page.graphql || finish
require_file GraphQlClubPage.java || finish
EXP=/course/modules/m01-why-graphql/contract/expected

# The operation must be valid on its own
out=$(jq -Rs '{query: ., variables: {id: "c1"}}' "$LAB_DIR/club-page.graphql" \
  | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
jq -e '.errors' >/dev/null 2>&1 <<<"$out" && fail "club-page.graphql has errors" "$(jq -c '.errors[0].message' <<<"$out")" \
  || pass "club-page.graphql is valid against the schema"

for c in c1 c2 c3; do
  ref_reset_stats
  if ! (cd "$LAB_DIR" && timeout 60 java -cp "$(lib_cp)" GraphQlClubPage.java "$c" >/tmp/out.json 2>/tmp/err.txt); then
    fail "$c: program failed"; tail -5 /tmp/err.txt | sed 's/^/        /'; continue
  fi
  if jq -e . /tmp/out.json >/dev/null 2>&1 && json_equal /tmp/out.json "$EXP/$c.json"; then pass "$c: page matches the contract"
  else fail "$c: page differs from contract/expected/$c.json (< yours, > expected)"; json_diff /tmp/out.json "$EXP/$c.json"; fi
  stats=$(ref_stats)
  [[ "$(jq .graphql <<<"$stats")" == 1 && "$(jq .api <<<"$stats")" == 0 ]] \
    && pass "$c: exactly 1 GraphQL request, no REST" || fail "$c: server counted $stats, expected {\"api\":0,\"graphql\":1}"
done
grep -qE '^\| GraphQL \| *[0-9]+' "$LAB_DIR/notes.md" 2>/dev/null && pass "notes.md has measurements (rubric: bin/course grade m01/03)" \
  || fail "notes.md: fill in the GraphQL row"
finish

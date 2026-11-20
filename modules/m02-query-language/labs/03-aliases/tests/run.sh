#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file compare-clubs.graphql || finish
require_file CompareClubs.java || finish

for pair in "c1 c2" "c3 c1"; do
  set -- $pair
  ref_reset_stats
  (cd "$LAB_DIR" && timeout 60 java -cp "$(lib_cp)" CompareClubs.java "$1" "$2" >/tmp/out.txt 2>/tmp/err.txt)
  if diff -q /tmp/out.txt "$LAB_HOME/tests/expected/$1-$2.txt" >/dev/null; then pass "$1 vs $2 matches"
  else fail "$1 vs $2 differs (< yours, > expected)"; diff /tmp/out.txt "$LAB_HOME/tests/expected/$1-$2.txt" | head -8 | sed 's/^/        /'
       [[ -s /tmp/err.txt ]] && tail -2 /tmp/err.txt | sed 's/^/        /'; fi
  [[ "$(ref_stats | jq .graphql)" == 1 ]] && pass "$1 vs $2: one request" || fail "$1 vs $2: expected exactly 1 GraphQL request, server counted $(ref_stats)"
done

if [[ -s "$LAB_DIR/collision.graphql" ]]; then
  out=$(jq -Rs '{query: ., variables: {a: "c1", b: "c2"}}' "$LAB_DIR/collision.graphql" \
    | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
  if jq -e '.errors[]?.message | test("FieldsConflict")' >/dev/null <<<"$out"; then pass "collision.graphql is rejected with FieldsConflict"
  else fail "collision.graphql should fail validation with FieldsConflict" "got: $(jq -c . <<<"$out" | cut -c1-160)"; fi
else fail "collision.graphql is missing"; fi

lines=$(grep -cvE '^\s*(#.*)?$' "$LAB_DIR/notes.md" 2>/dev/null || echo 0)
(( lines >= 9 )) && pass "notes.md filled in (rubric: bin/course grade m02/03)" || fail "notes.md: answer the three questions"
finish

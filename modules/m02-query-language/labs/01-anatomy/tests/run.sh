#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file club-card.graphql || finish
require_file ClubCard.java || finish

out=$(jq -Rs '{query: ., variables: {id: "c1"}}' "$LAB_DIR/club-card.graphql" \
  | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
jq -e '.errors' >/dev/null 2>&1 <<<"$out" && fail "club-card.graphql has errors" "$(jq -c '.errors[0].message' <<<"$out")" \
  || pass "club-card.graphql is valid against the schema"

for c in c1 c2 c3 zz; do
  (cd "$LAB_DIR" && timeout 60 java -cp "$(lib_cp)" ClubCard.java "$c" >/tmp/out.txt 2>/tmp/err.txt)
  if diff -q /tmp/out.txt "$LAB_HOME/tests/expected/$c.txt" >/dev/null; then pass "$c: card matches"
  else fail "$c: card differs (< yours, > expected)"; diff /tmp/out.txt "$LAB_HOME/tests/expected/$c.txt" | head -8 | sed 's/^/        /'
       [[ -s /tmp/err.txt ]] && tail -3 /tmp/err.txt | sed 's/^/        /'; fi
done

grep -q 'record Owner\|record User' "$LAB_DIR/ClubCard.java" && grep -q 'record Book' "$LAB_DIR/ClubCard.java" \
  && pass "nested selections have their own records" || fail "map owner and currentBook to their own records, not to Map or JsonNode"

lines=$(grep -cvE '^\s*(#.*)?$' "$LAB_DIR/notes.md" 2>/dev/null || echo 0)
(( lines >= 14 )) && pass "notes.md filled in (rubric: bin/course grade m02/01)" || fail "notes.md: write up the three experiments"
finish

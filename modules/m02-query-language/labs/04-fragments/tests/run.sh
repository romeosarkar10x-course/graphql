#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file profile-page.graphql || finish
doc="$LAB_DIR/profile-page.graphql"

defs=$(grep -cE '^\s*fragment +[A-Za-z_][A-Za-z0-9_]* +on +' "$doc")
(( defs >= 4 )) && pass "$defs named fragments" || fail "found $defs named fragment definitions, need at least 4"
reused=$(grep -oE '\.\.\.[A-Za-z_][A-Za-z0-9_]*' "$doc" | sort | uniq -c | awk '$1 >= 2' | wc -l)
(( reused >= 1 )) && pass "at least one fragment is spread in two places" || fail "no fragment is reused: spread one in at least two places"
body=$(grep -vE '^\s*#' "$doc" | grep -cvE '^\s*$')
(( body <= 60 )) && pass "document is $body lines (the original is 70)" || fail "document is $body lines; fragments should make it shorter than the original 70"

send() { jq -Rs --arg id "$2" --arg q "$3" '{query: ., variables: {id: $id, q: $q}}' "$1" \
  | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql" | jq -c .; }
for v in "u1 the" "u6 mid"; do
  set -- $v
  send "$doc" "$1" "$2" >/tmp/out.json
  if cmp -s /tmp/out.json "$LAB_HOME/tests/expected/$1-$2.json"; then pass "user $1, search '$2': byte-identical to the original"
  elif json_equal /tmp/out.json "$LAB_HOME/tests/expected/$1-$2.json"; then
    fail "user $1, search '$2': same data but different key order" "fields come back in the order they are first selected"
  else fail "user $1, search '$2': response differs (< yours, > original)"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/$1-$2.json"; fi
done

if [[ -s "$LAB_DIR/fragment-cycle.graphql" ]]; then
  out=$(jq -Rs '{query: ., variables: {id: "c1"}}' "$LAB_DIR/fragment-cycle.graphql" \
    | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
  jq -e '.errors[]?.message | test("FragmentCycle")' >/dev/null <<<"$out" && pass "fragment-cycle.graphql is rejected with FragmentCycle" \
    || fail "fragment-cycle.graphql should fail validation with FragmentCycle" "got: $(jq -c . <<<"$out" | cut -c1-160)"
else fail "fragment-cycle.graphql is missing"; fi

lines=$(grep -cvE '^\s*(#.*)?$' "$LAB_DIR/notes.md" 2>/dev/null || echo 0)
(( lines >= 9 )) && pass "notes.md filled in (rubric: bin/course grade m02/04)" || fail "notes.md: answer the questions"
finish

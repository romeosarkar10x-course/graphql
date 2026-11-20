#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file first-query.jsh || finish
f="$LAB_DIR/first-query.jsh"

ref_reset_stats
out=$(cd "$LAB_DIR" && timeout 60 jshell --class-path "$(lib_cp)" -q first-query.jsh </dev/null 2>&1)
last=$(grep -vE '^\s*$' <<<"$out" | tail -1)
if [[ "$last" == "The Left Hand of Darkness" ]]; then pass "prints the current book title"
else fail "expected the last line to be 'The Left Hand of Darkness'" "got: ${last:-<nothing>}"
     grep -iE 'error|exception' <<<"$out" | head -5 | sed 's/^/        /'; fi

n=$(ref_stats | jq .graphql)
[[ "$n" == 1 ]] && pass "made exactly 1 GraphQL request" || fail "made $n GraphQL requests, expected 1"

grep -q '"variables"' "$f" && grep -qE '\$[a-zA-Z]+ *: *ID' "$f" \
  && pass "uses a GraphQL variable" \
  || fail "no GraphQL variable found" "declare \$clubId: ID! in the operation and send it under \"variables\""
grep -qE 'id: *\\?"c1' "$f" && fail "c1 is still glued into the query string"

s="$LAB_DIR/status-codes.txt"
for k in GET TEXT_PLAIN NO_BODY; do
  v=$(grep -E "^$k=" "$s" 2>/dev/null | cut -d= -f2 | tr -d ' ')
  # Expected values are the reference server's real answers; re-measured here so the test cannot drift.
  case $k in
    GET) want=$(curl -s -o /dev/null -w '%{http_code}' -G "$REF/graphql" --data-urlencode 'query={ hello }') ;;
    TEXT_PLAIN) want=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$REF/graphql" -H 'Content-Type: text/plain' -d '{"query":"{ hello }"}') ;;
    NO_BODY) want=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$REF/graphql" -H 'Content-Type: application/json') ;;
  esac
  [[ "$v" == "$want" ]] && pass "status-codes.txt: $k=$want" || fail "status-codes.txt: $k is '${v:-missing}'" "measure it again"
done

[[ $(wc -w < "$LAB_DIR/notes.md") -gt 90 ]] && pass "notes.md filled in (rubric: bin/course grade m00/04)" \
  || fail "notes.md not filled in yet"
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
right=0
for n in 01 02 03 04 05 06 07 08; do
  [[ -s predictions/$n.json ]] || { fail "predictions/$n.json missing"; continue; }
  actual=$(timeout 60 java -cp "$(lib_cp)" "$LAB_HOME/solution/Nullability.java" "$LAB_HOME/solution/scenarios/$n.json" | jq -S '.data')
  if jq . predictions/$n.json >/dev/null 2>&1 && [[ "$(jq -S . predictions/$n.json)" == "$actual" ]]; then right=$((right+1)); pass "scenario $n predicted"
  else fail "scenario $n: prediction does not match. Run: bin/course java Nullability.java scenarios/$n.json"; fi
done
note "predictions right: $right/8"

# The fix: same failures, your schema, the page must survive.
require_file fix.graphqls || finish
out=$(timeout 60 java -cp "$(lib_cp)" "$LAB_HOME/solution/Nullability.java" "$LAB_HOME/solution/scenarios/bad-day.json" fix.graphqls 2>/tmp/err.txt)
if [[ -z "$out" ]]; then fail "fix.graphqls does not build" "$(tail -2 /tmp/err.txt)"; finish; fi
jq -e '.data.club.name == "The Midnight Readers" and .data.club.owner.name == "Kemi Adeyemi"' <<<"$out" >/dev/null \
  && pass "bad day: club name and owner still render" || fail "bad day: the club itself is lost" "data: $(jq -c .data <<<"$out" | cut -c1-160)"
jq -e '[.data.club.members[]? | select(. != null) | .name | select(. != null)] | index("Asha Rao") != null' <<<"$out" >/dev/null \
  && pass "bad day: the healthy members still render" || fail "bad day: one bad member takes the others with it"
jq -e '.data.club.currentBook.title == "The Left Hand of Darkness"' <<<"$out" >/dev/null \
  && pass "bad day: the current book's title still renders" || fail "bad day: the ratings outage hides the whole book"
lines=$(java -cp "$(lib_cp)" /course/tools/gql/Gql.java schema-lines fix.graphqls 2>/dev/null)
ok=1; for must in 'Club.id: ID!' 'Club.name: String!' 'User.id: ID!' 'Book.title: String!' 'Query.club(id: ID!): Club'; do
  grep -qxF "$must" <<<"$lines" || { ok=0; fail "fix.graphqls must keep $must"; }
done
(( ok )) && pass "fix.graphqls keeps the guarantees clients rely on"
notes_check notes.md 60 m03/04
finish

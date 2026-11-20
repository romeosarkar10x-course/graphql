#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Lookup.java || finish
cd "$LAB_DIR"
# Outcome of one case: "ok <data>" or "rejected/<stage>" (same idea as lab 3.3's table)
outcome() {
  jq -r 'if (.errors // []) | length > 0 then
           (.errors[0].message) as $m
           | "rejected/" + (if ($m | startswith("Validation error")) then "validation"
                            elif ($m | startswith("Variable ")) then "variable-coercion"
                            elif (.data == null and ($m | test("OneOf|Exactly one"))) then "variable-coercion"
                            else "execution" end)
         else "ok " + (.data | tostring) end'
}
: >/tmp/table.txt
while read -r line; do
  name=$(jq -r .name <<<"$line"); jq -r .query <<<"$line" >/tmp/q.graphql
  timeout 60 java -cp "$(lib_cp)" Lookup.java /tmp/q.graphql "$(jq -c .variables <<<"$line")" 2>/tmp/err.txt | outcome >>/tmp/table.txt.part || echo "crashed" >>/tmp/table.txt.part
  echo "$name $(tail -1 /tmp/table.txt.part)" >>/tmp/table.txt
done <"$LAB_HOME/tests/cases.jsonl"
rm -f /tmp/table.txt.part
if diff -q /tmp/table.txt "$LAB_HOME/tests/expected/table.txt" >/dev/null; then pass "all 14 lookups: right outcome at the right stage"
else fail "lookup outcomes differ (< yours, > expected)"; diff /tmp/table.txt "$LAB_HOME/tests/expected/table.txt" | sed 's/^/        /' | head -16; fi
grep -qE 'input\s+BookLookup\s+@oneOf' schema.graphqls && pass "BookLookup is @oneOf" || fail "BookLookup should be declared @oneOf"
for f in cycle inhabited; do [[ -s $f.graphqls ]] || { fail "$f.graphqls missing"; continue; }; done
r=$(java -cp "$(lib_cp)" Lookup.java --schema cycle.graphqls 2>&1)
grep -q '@oneOf' cycle.graphqls && [[ "$r" == *"must be inhabited"* ]] && pass "cycle.graphqls: graphql-java rejects the uninhabitable oneOf" \
  || fail "cycle.graphqls should be a @oneOf input graphql-java rejects as uninhabited" "got: $(cut -c1-140 <<<"$r")"
r=$(java -cp "$(lib_cp)" Lookup.java --schema inhabited.graphqls 2>&1)
grep -q '@oneOf' inhabited.graphqls && [[ "$r" == BUILT ]] && pass "inhabited.graphqls: a self-referencing oneOf that builds" \
  || fail "inhabited.graphqls should keep the self-reference, stay @oneOf, and build" "got: $(cut -c1-140 <<<"$r")"
notes_check notes.md 60 m03/08
finish

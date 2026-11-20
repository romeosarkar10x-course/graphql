#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
gql() { java -cp "$(lib_cp)" /course/tools/gql/Gql.java "$@"; }
[[ -d schema ]] || { fail "schema/ is missing"; finish; }
files=$(ls schema/*.graphqls 2>/dev/null | wc -l)
(( files >= 5 )) && pass "schema/ has $files files" || fail "split the schema into at least 5 .graphqls files (found $files)"
if ! gql schema-lines schema >/dev/null 2>/tmp/err.txt; then fail "schema/ does not build when merged" "$(head -3 /tmp/err.txt)"; finish; fi
pass "schema/ merges and builds"
stats=$(gql stats schema)
types=$(jq .types <<<"$stats")
(( types >= 15 )) && pass "$types named types (15+)" || fail "the full Shelfie schema needs 15+ named types, found $types"
ext=$(cat schema/*.graphqls | grep -cE '^\s*extend\s+(type|interface|union|enum|input|scalar|schema)')
(( ext >= 4 )) && pass "$ext type extensions" || fail "use at least 4 extend definitions across files, found $ext"
qfiles=$(grep -lE '^\s*(extend\s+)?type\s+Query\b' schema/*.graphqls | wc -l)
(( qfiles >= 3 )) && pass "Query gets fields from $qfiles files" || fail "at least 3 files should define or extend Query, found $qfiles"
for k in fields arguments inputFields enumValues; do
  n=$(jq ".deprecated.$k" <<<"$stats")
  (( n >= 1 )) && pass "deprecated $k: $n" || fail "deprecate at least one of: $k"
done
if cat schema/*.graphqls | grep -P '@deprecated(?!\s*\(\s*reason\s*:)' >/dev/null; then fail "every @deprecated needs a reason: saying what to use instead is the point"
else pass "every @deprecated has a reason"; fi
[[ -s lint.json ]] || { fail "lint.json is missing"; finish; }
mapfile -t coords < <(jq -r '.rules[].coordinate' lint.json)
(( ${#coords[@]} >= 10 )) && pass "lint.json has ${#coords[@]} rules" || fail "lint.json needs 10+ rules, found ${#coords[@]}"
out=$(gql coordinates schema "${coords[@]}"); rc=$?
(( rc == 0 )) && pass "every lint coordinate resolves in schema/" || fail "lint coordinates that name nothing:" "$(grep ^missing <<<"$out" | tr '\n' ' ')"
kinds=$(awk '$1=="ok"{print $3}' <<<"$out" | sort -u | wc -l)
(( kinds >= 4 )) && pass "lint coordinates cover $kinds kinds of element" || fail "use at least 4 kinds of coordinate (type, field, argument, enum value, input field, directive...), found $kinds"
notes_check notes.md 60 m03/09
finish

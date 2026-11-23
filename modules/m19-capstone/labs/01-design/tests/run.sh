#!/usr/bin/env bash
# Capstone stage 1: pitch, design doc and contract. Static checks; the design review is rubric-graded.
source /course/tools/testlib.sh
source /course/modules/m19-capstone/contract/capstone.sh
source /course/modules/m19-capstone/contract/docs.sh
cap_manifest || finish

echo "  -- the pitch and the design"
doc_check pitch 150 "the pitch"
doc_check design 800 "the design doc" 'schema|types' 'architecture' 'nullab' 'error' 'auth'
f=$(m .docs.design)
grep -qE '^```mermaid|!\[[^]]*\]\([^)]+\.(svg|png)\)' "$LAB_DIR/$f" 2>/dev/null && pass "$f: an architecture diagram" \
  || fail "$f: an architecture diagram (a mermaid block or an svg/png image)"
adr_check

echo "  -- the contract"
cap_api_schema || finish
R=$(capcheck schema "$CAP_TMP/api.graphql")
t=$(jq .types <<<"$R"); (( t >= 15 )) && pass "$t types (15 or more)" || fail "at least 15 types of your own ($t now)"
jq -e '[.interfaces[] | select(. >= 2)] | length > 0' <<<"$R" >/dev/null && pass "an interface with two or more implementations: $(jq -c .interfaces <<<"$R")" \
  || fail "an interface with at least two implementations"
jq -e '.unions | length > 0' <<<"$R" >/dev/null && pass "a union: $(jq -c .unions <<<"$R")" || fail "a union"
jq -e '.oneOf | length > 0' <<<"$R" >/dev/null && pass "a @oneOf input: $(jq -c .oneOf <<<"$R")" || fail "a @oneOf input"
jq -e '.filteredSortedFields | length > 0' <<<"$R" >/dev/null && pass "a cursor connection with filter and sort: $(jq -c .filteredSortedFields <<<"$R")" \
  || fail "a cursor connection (first/after) with a filter input and a sort argument" "connections: $(jq -c .connections <<<"$R"), paged: $(jq -c .pagedFields <<<"$R")"
jq -e '.typedErrorMutations | length > 0' <<<"$R" >/dev/null && pass "mutations with typed errors: $(jq -c .typedErrorMutations <<<"$R")" \
  || fail "a mutation whose result carries expected failures as data (a union, or a payload with errors)"
d=$(jq .describedTypes <<<"$R"); (( d * 10 >= t * 8 )) && pass "$d of $t types have descriptions" || fail "descriptions on at least 80% of types ($d of $t)"

O=$(capcheck ops "$CAP_TMP/api.graphql" "$LAB_DIR/$(m .operations)")
n=$(jq '.operations | length' <<<"$O"); bad=$(jq -c '[.parseErrors[], (.operations[] | select(.errors | length > 0) | "\(.name): \(.errors[0])")]' <<<"$O")
[[ $bad == "[]" ]] && pass "$n operations validate against the composed schema" || fail "operations that do not validate" "$bad"
(( n >= 8 )) && pass "8 or more operations ($n)" || fail "at least 8 named operations, one per screen or job ($n now)"
jq -e '[.operations[] | select(.name == null)] | length == 0' <<<"$O" >/dev/null || fail "every operation needs a name"
jq -e '[.operations[] | select(.kind == "mutation")] | length >= 2' <<<"$O" >/dev/null && pass "two or more mutations" || fail "at least two mutations among the operations"
jq -e '[.operations[] | select(.kind == "subscription" or .defer)] | length > 0' <<<"$O" >/dev/null && pass "a real-time operation (subscription or @defer)" \
  || fail "one real-time operation: a subscription, or a query with @defer"
h=$(m .hurl.file); c=$(grep -cE '^(GET|POST) ' "$LAB_DIR/$h" 2>/dev/null)
(( c >= 10 )) && pass "the Hurl suite $h: $c requests" || fail "a Hurl conformance suite with at least 10 requests (${c:-0} in ${h:-nothing})"

echo "  -- advisory (Module 7's design lint; not graded here, the reviewer reads it)"
java -cp "$(lib_cp)" /course/tools/design/Design.java lint "$CAP_TMP/api.graphql" --rules naming,references,json-scalars,boolean-args,db-names,crud-names 2>&1 \
  | grep '^fail' | head -8 | sed 's/^/        /'
echo "  -- the review"
notes_check "$(m .docs.review)" 200 m19/01
finish

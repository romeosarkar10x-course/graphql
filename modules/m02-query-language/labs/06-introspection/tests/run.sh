#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file SchemaDump.java || finish
GQL="java -cp $(lib_cp) /course/tools/gql/Gql.java"

# 1. Dump the reference schema; it must describe exactly what the server's own SDL describes.
(cd "$LAB_DIR" && timeout 90 java -cp "$(lib_cp)" SchemaDump.java "$REF/graphql" >/tmp/dump.graphqls 2>/tmp/err.txt)
if grep -q '^type Club' /tmp/dump.graphqls; then
  curl -fsS "$REF/graphql/schema" >/tmp/ref.graphqls
  if diff <($GQL schema-lines /tmp/dump.graphqls 2>&1) <($GQL schema-lines /tmp/ref.graphqls) >/tmp/schema.diff; then
    pass "dumped schema matches the server's SDL ($(wc -l </tmp/dump.graphqls) lines)"
  else fail "dumped schema differs from $REF/graphql/schema (< yours, > server)"; head -10 /tmp/schema.diff | sed 's/^/        /'; fi
else fail "SchemaDump.java did not print SDL with a Club type"; head -3 /tmp/dump.graphqls /tmp/err.txt | sed 's/^/        /'; fi

(cd "$LAB_DIR" && timeout 90 java -cp "$(lib_cp)" SchemaDump.java "$REF/graphql" --count >/tmp/count.txt 2>/dev/null)
want=$(curl -sS -H 'Content-Type: application/json' -d '{"query":"{ __schema { types { name } } }"}' "$REF/graphql" \
  | jq '[.data.__schema.types[].name | select(startswith("__") | not)] | length')
grep -qx "$want types" /tmp/count.txt && pass "--count says $want types for Shelfie" || fail "--count should print '$want types' for Shelfie" "got: $(head -1 /tmp/count.txt)"

# 2. The hand-written __type query.
if require_file type-of.graphql; then
  out=$(jq -Rs '{query: .}' "$LAB_DIR/type-of.graphql" | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
  owner=$(jq -c '.data.__type.fields[]? | select(.name == "owner") | .type | [.kind, .ofType.kind, .ofType.name]' <<<"$out")
  members=$(jq -c '.data.__type.fields[]? | select(.name == "members") | [.args[0].name, .args[0].defaultValue, .type.kind, .type.ofType.kind, .type.ofType.ofType.kind, .type.ofType.ofType.ofType.name]' <<<"$out")
  [[ "$owner" == '["NON_NULL","OBJECT","User"]' ]] && pass "__type: owner is NON_NULL of OBJECT User" || fail "__type query: Club.owner's type should unwrap to NON_NULL > OBJECT User" "got: ${owner:-nothing}"
  [[ "$members" == '["first","10","NON_NULL","LIST","NON_NULL","User"]' ]] && pass "__type: members(first = 10) is [User!]!, unwrapped through ofType" \
    || fail "__type query: Club.members needs args (name, defaultValue) and three levels of ofType" "got: ${members:-nothing}"
fi

# 3. Public APIs (optional: needs internet).
if curl -fsS -m 5 -o /dev/null https://countries.trevorblades.com/ -H 'Content-Type: application/json' -d '{"query":"{__typename}"}' 2>/dev/null; then
  (cd "$LAB_DIR" && timeout 90 java -cp "$(lib_cp)" SchemaDump.java https://countries.trevorblades.com/ --count >/tmp/c.txt 2>/dev/null)
  grep -qE '^[0-9]+ types$' /tmp/c.txt && pass "Countries API: $(cat /tmp/c.txt)" || fail "Countries API dump failed"
else note "skipped the Countries API check: no internet"; fi

lines=$(grep -cvE '^\s*(#.*)?$' "$LAB_DIR/notes.md" 2>/dev/null || echo 0)
(( lines >= 8 )) && pass "notes.md filled in (rubric: bin/course grade m02/06)" || fail "notes.md: fill in the type counts and questions"
finish

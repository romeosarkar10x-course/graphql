#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file calls.sh || finish
for c in c1 c3; do
  name=$(curl -fsS "$REF/api/clubs/$c" | jq -r .name); members=$(curl -fsS "$REF/api/clubs/$c" | jq '.memberIds|length')
  out=$(cd "$LAB_DIR" && REF="$REF" GRPC=shelfie-ref:9090 timeout 30 bash calls.sh "$c" 2>&1)
  for style in REST GRPC GRAPHQL; do
    grep -qxF "$style: $name" <<<"$out" && pass "$c $style prints '$name'" || fail "$c $style line wrong" "$(grep "^$style:" <<<"$out" | head -1)"
  done
  grep -qxF "GRPC_PAGE_MEMBERS: $members" <<<"$out" && pass "$c GetClubPage member count" || fail "$c GRPC_PAGE_MEMBERS should be $members"
done
grep -q 'variables' "$LAB_DIR/calls.sh" && pass "GraphQL call uses variables" || fail "GraphQL call should pass the id as a variable"
m="$LAB_DIR/tradeoff-matrix.md"
[[ -s "$m" ]] && (( $(grep -cE '^\*\*Pick:\*\* *[A-Za-z]' "$m") >= 5 )) \
  && pass "tradeoff-matrix.md has 5 picks (rubric: bin/course grade m01/04)" || fail "tradeoff-matrix.md: make a **Pick:** for all 5 scenarios"
finish

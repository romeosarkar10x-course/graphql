#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m17-under-the-hood/contract/harness.sh
cd "$LAB_DIR" || exit 1
lab_build
harness_expect "30 conformance tests" "Conformance $M17/conformance" '^passed 30 of 30$'
out=$(harness Parallel $M17/conformance/fixture.graphqls 2>&1 | tail -1)
ms=$(jq -r '.ms // 99999' <<<"$out" 2>/dev/null); virt=$(jq -r '.virtual // 0' <<<"$out" 2>/dev/null); right=$(jq -r '.result // ""' <<<"$out" 2>/dev/null)
[[ "$right" == right ]] && (( ms < 500 )) && pass "query fields and list items run in parallel: 7 resolvers of 200 ms took ${ms} ms (one after another: 1,400)" \
  || fail "query fields and list items run in parallel (7 resolvers of 200 ms in under 500 ms)" "${out:0:300}"
[[ "$virt" == 7 ]] && pass "all 7 slow resolvers ran on virtual threads" || fail "resolvers run on virtual threads" "${virt:-0} of 7 did"
out=$(harness Smoke /course/modules/m02-query-language/labs/07-documents/solution/ops/*.graphql 2>&1)
n=$(grep -c '^same ' <<<"$out")
(( n == 12 )) && pass "Module 2's 12 operations: the same responses as graphql-java over the reference data" \
  || { fail "Module 2's operations: $n of 12 give graphql-java's responses"; grep -A2 '^DIFF' <<<"$out" | head -9 | sed 's/^/        /'; }
finish

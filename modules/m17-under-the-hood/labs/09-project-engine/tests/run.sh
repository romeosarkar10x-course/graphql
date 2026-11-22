#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m17-under-the-hood/contract/harness.sh
cd "$LAB_DIR" || exit 1
lab_build
n=$(bash /course/modules/m05-graphql-java/labs/09-project-tiny-server/tests/count-lines.sh src/main/java)
(( n < 2000 )) && pass "$n lines of engine code (under 2,000; blank, comment, package and import lines not counted)" \
  || fail "$n lines of engine code: the brief allows under 2,000"
harness_expect "lexer: fixed cases" "Cases lexer" '^passed 20 of 20$'
harness_expect "lexer: differential, 1,000 documents" "TokenDiff 1000 17" '^agree [0-9]+ of 1000; disagree 0; '
harness_expect "parser: fixed cases" "Cases parser" '^passed 21 of 21$'
harness_expect "parser: differential, 1,000 documents" "ParseDiff 1000 17" '^agree [0-9]+ of 1000; disagree 0; '
harness_expect "validation: 40 invalid documents" "ValidateDiff $M17/validation/invalid" '^documents 40 of 40 agree'
harness_expect "validation: 20 valid documents" "ValidateDiff $M17/validation/valid /course/modules/m02-query-language/labs/07-documents/solution/ops" '^documents 20 of 20 agree; errors yours 0 '
harness_expect "execution: 30 conformance tests" "Conformance $M17/conformance" '^passed 30 of 30$'
out=$(harness RoundTrip /course/reference/shelfie-ref/src/main/resources/graphql/schema.graphqls $M17/conformance/fixture.graphqls $M17/printing/kitchen-sink.graphqls 2>&1)
if (( $(grep -c '^pass' <<<"$out") == 12 )); then pass "introspection and printing: 12 round-trip checks"
else fail "introspection and printing: round trips" "$(grep -v '^pass' <<<"$out" | head -4)"; fi
require_file DESIGN.md "how your engine is put together" && notes_check DESIGN.md 120 m17/09
finish

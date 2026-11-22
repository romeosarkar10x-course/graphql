#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m17-under-the-hood/contract/harness.sh
cd "$LAB_DIR" || exit 1
lab_build
harness_expect "21 fixed cases, spec behaviour where graphql-java differs" "Cases parser" '^passed 21 of 21$'
harness_expect "differential test against graphql-java's parser on 1,000 documents (trees and locations)" "ParseDiff 1000 17" '^agree [0-9]+ of 1000; disagree 0; '
# The fuzz test in regression mode: replays the inputs Jazzer saved (src/test/resources/...Inputs) and its corpus.
if mvn -q test -Dtest=ParserFuzzTest >/tmp/m17-fuzz.log 2>&1; then pass "ParserFuzzTest replays its saved inputs without a crash (mvn test, no fuzzing)"
else fail "ParserFuzzTest fails on its saved inputs" "$(grep -E 'Tests run|FuzzTestFinding|Exception' /tmp/m17-fuzz.log | head -5)"; fi
require_file FUZZ.md "your fuzz report" && notes_check FUZZ.md 120 m17/03
finish

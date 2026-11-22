#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m13-testing/contract/suite-test.sh
cd "$LAB_DIR" || exit 1
suite_tests_using '^\s*@(ExtendWith\(MockitoExtension|Mock\b)' "a Mockito unit test (no Spring) exists"
grep -rlE '@(SpringBootTest|GraphQlTest|WebMvcTest)' src/test/java/dev/shelfie/app/service 2>/dev/null | grep -q . \
  && fail "service tests start Spring" "tests under src/test/java/dev/shelfie/app/service must construct the service with mocks, no Spring context" \
  || pass "service tests run without a Spring context"
suite_tests_using '^\s*@GraphQlTest' "a @GraphQlTest slice test exists"
suite_prepare /course/modules/m13-testing/contract/shelfie-app || finish
suite_baseline || finish
suite_mutants "$LAB_HOME/tests/mutants.json"
note "killed $SUITE_KILLED of $SUITE_TOTAL planted bugs"
notes_check notes.md 60 m13/01
finish

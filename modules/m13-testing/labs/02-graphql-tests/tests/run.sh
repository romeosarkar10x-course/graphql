#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m13-testing/contract/suite-test.sh
cd "$LAB_DIR" || exit 1
n=$(ls src/test/resources/graphql-test/*.graphql 2>/dev/null | wc -l)
(( n >= 3 )) && pass "$n document files in src/test/resources/graphql-test" || fail "at least 3 document files in src/test/resources/graphql-test (found $n)"
suite_tests_using '\.documentName\(' "tests load documents by name (documentName)"
suite_tests_using '^[^/]*ExecutionGraphQlServiceTester' "a test uses ExecutionGraphQlServiceTester (in-process)"
suite_tests_using '^[^/]*HttpGraphQlTester' "a test uses HttpGraphQlTester (through the web layer)"
suite_tests_using '^[^/]*WebSocketGraphQlTester' "a test uses WebSocketGraphQlTester (a subscription)"
suite_tests_using '\.errors\(\)' "a test asserts on GraphQL errors"
grep -rqE '^[^/]*DgsQueryExecutor' dgs/src/test 2>/dev/null && pass "a DGS test uses DgsQueryExecutor" || fail "a DGS test uses DgsQueryExecutor (dgs/src/test)"
M13A=/course/modules/m13-testing/contract
suite_prepare $M13A/shelfie-app || finish
suite_baseline || finish
suite_mutants "$LAB_HOME/tests/mutants.json"
spring_killed=$SUITE_KILLED spring_total=$SUITE_TOTAL
suite_prepare $M13A/dgs-app "$LAB_DIR/dgs" && suite_baseline && suite_mutants "$LAB_HOME/tests/dgs-mutants.json"
note "killed $SUITE_KILLED of $SUITE_TOTAL planted bugs (Spring app and DGS app)"
notes_check notes.md 60 m13/02
finish

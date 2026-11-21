#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
C=/course/modules/m10-security/contract
app_build target/shelfie-secure.jar || finish
app_start java -jar target/shelfie-secure.jar || finish
app_hurl "$LAB_HOME/tests/cop.hurl" "graphql-cop's HIGH findings are fixed (cop.hurl)" --file-root "$LAB_HOME/tests"
suite() { # $1 url -> "<tests> <failures+errors>"
  rm -rf target/surefire-reports
  mvn -q test -Dshelfie.url="$1" >/tmp/suite.log 2>&1
  cat target/surefire-reports/TEST-*.xml 2>/dev/null | grep -oE '<testsuite [^>]*' | head -1 \
    | sed -E 's/.*tests="([0-9]+)".*failures="([0-9]+)".*/\1 \2/;t;d' | awk '{print $1, $2}'
}
read -r n f <<<"$(suite "$URL")"; errs=$(grep -oE 'errors="[0-9]+"' target/surefire-reports/TEST-*.xml 2>/dev/null | head -1 | grep -oE '[0-9]+')
(( ${n:-0} >= 10 && ${f:-1} == 0 && ${errs:-1} == 0 )) && pass "your regression suite: $n tests, all pass against your server" \
  || fail "your regression suite: 10+ tests, all passing against your server (got ${n:-0} tests, ${f:-?} failed, ${errs:-?} errors)" "$(grep -E 'FAIL|ERROR\]' /tmp/suite.log | head -3)"
$C/vulnerable.sh start 4100 >/dev/null 2>&1 || { fail "the vulnerable fork starts on :4100"; finish; }
read -r n2 f2 <<<"$(suite http://localhost:4100/graphql)"; e2=$(grep -oE 'errors="[0-9]+"' target/surefire-reports/TEST-*.xml 2>/dev/null | head -1 | grep -oE '[0-9]+')
$C/vulnerable.sh stop
caught=$(( ${f2:-0} + ${e2:-0} ))
(( caught >= 8 )) && pass "the same suite catches the vulnerable fork: $caught of ${n2:-0} tests fail there" \
  || fail "the same suite catches the vulnerable fork: 8+ tests must fail against it (got $caught)" "a test that passes on both servers proves nothing"
app_stop
before=$(jq '[.[] | select(.result == true)] | length' scans/before.json 2>/dev/null || echo -1)
after=$(jq '[.[] | select(.result == true and (.severity == "HIGH" or .severity == "MEDIUM"))] | length' scans/after.json 2>/dev/null || echo -1)
(( before >= 3 )) && pass "scans/before.json: graphql-cop's findings before ($before)" || fail "scans/before.json holds graphql-cop's JSON output from before your fixes (3+ findings)"
[[ "$after" == 0 ]] && jq -e 'length >= 10' scans/after.json >/dev/null 2>&1 && pass "scans/after.json: no HIGH or MEDIUM findings left" \
  || fail "scans/after.json: graphql-cop's full output after your fixes, with no HIGH or MEDIUM finding"
notes_check notes.md 60 m10/08
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
java -cp "$(lib_cp)" /course/modules/m08-pagination-errors/contract/Catalog.java m08_04 >/dev/null || { fail "could not load the catalogue into m08_04"; finish; }
app_build target/shelfie-filters.jar || finish
app_start java -jar target/shelfie-filters.jar || finish
if out=$(java -cp "$(lib_cp)" "$LAB_HOME/tests/FilterOracle.java" "$URL" 2>&1); then pass "filters, sorts and search agree with the oracle: $(tail -1 <<<"$out")"
else fail "filters, sorts and search: $(tail -1 <<<"$out")"; grep '^FAIL' <<<"$out" | sed 's/^FAIL  /        /'; fi
app_hurl "$LAB_HOME/tests/lab.hurl" "filter API (lab.hurl)"
app_stop
# F06 searched for "quixotic"; with show-sql on, a bound parameter never appears in the log.
if grep -q quixotic /tmp/app.log; then
  fail "the search text is a bind parameter (?), not text inside the SQL" "$(grep -m1 -oE '.{0,60}quixotic.{0,10}' /tmp/app.log)"
else pass "the search text is a bind parameter (?), not text inside the SQL"; fi

# Your own property tests: jqwik, 1,000 tries per property, all green.
if ! grep -rqE '@Property\(\s*tries\s*=\s*1_?000\s*\)' src/test/java 2>/dev/null; then
  fail "src/test/java has jqwik properties with @Property(tries = 1000)"
elif mvn -q test >/tmp/props.log 2>&1; then
  n=$(grep -cE '^checks = ' /tmp/props.log)
  if (( n >= 3 )); then pass "your property tests pass ($n properties)"; else fail "write at least 3 properties (found $n)"; fi
else fail "your property tests fail" "$(grep -E 'Tests run:|FAILURE|expected' /tmp/props.log | head -4)"; fi
java -cp "$(lib_cp)" /course/modules/m08-pagination-errors/contract/Catalog.java m08_04 >/dev/null
notes_check notes.md 60 m08/04
finish

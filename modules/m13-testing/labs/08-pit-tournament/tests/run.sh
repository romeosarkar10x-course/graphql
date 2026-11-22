#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m13-testing/contract/suite-test.sh
cd "$LAB_DIR" || exit 1
TARGET=${PIT_TARGET:-80}
# Your tests, the contract app's services: PIT mutates dev.shelfie.app.service.* and runs every test per mutant.
suite_prepare /course/modules/m13-testing/contract/shelfie-app || finish
suite_baseline || finish
if ! (cd "$SUITE" && mvn -q -o org.pitest:pitest-maven:mutationCoverage >"$SUITE/pit.log" 2>&1 || mvn -q org.pitest:pitest-maven:mutationCoverage >"$SUITE/pit.log" 2>&1); then
  fail "PIT did not finish" "$(grep -E 'ERROR|Exception' "$SUITE/pit.log" | head -3)"; finish
fi
read -r total detected survived nocov < <(python3 - "$SUITE/target/pit-reports/mutations.xml" <<'PY'
import sys, xml.etree.ElementTree as ET, collections
c = collections.Counter(m.get("status") for m in ET.parse(sys.argv[1]).getroot())
print(sum(c.values()), c["KILLED"] + c["TIMED_OUT"] + c["MEMORY_ERROR"] + c["RUN_ERROR"], c["SURVIVED"], c["NO_COVERAGE"])
PY
)
score=$(( total ? 100 * detected / total : 0 ))
note "PIT: $total mutants, $detected killed, $survived survived, $nocov never run by any test"
python3 - "$SUITE/target/pit-reports/mutations.xml" <<'PY' | sed 's/^/        /'
import sys, xml.etree.ElementTree as ET
left = [m for m in ET.parse(sys.argv[1]).getroot() if m.get("status") in ("SURVIVED", "NO_COVERAGE")]
for m in left[:6]:
    print(f'{m.get("status").lower():11} {m.findtext("mutatedClass").rsplit(".",1)[-1]}.{m.findtext("mutatedMethod")} line {m.findtext("lineNumber")}: {m.findtext("description")}')
if len(left) > 6: print(f"... and {len(left) - 6} more (open target/pit-reports/index.html after running PIT yourself)")
PY
(( score >= TARGET )) && pass "mutation score ${score}% (target ${TARGET}%)" || fail "mutation score ${score}%: the target is ${TARGET}%"
mkdir -p "$LAB_DIR/target" && echo "$score $total $detected $(date -u +%FT%TZ)" > "$LAB_DIR/target/pit-score.txt"
finish

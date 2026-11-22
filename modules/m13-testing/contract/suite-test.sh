# Shared by Module 13 tests that grade a learner's *tests*: source it after testlib.sh.
# The learner's src/test runs against the contract app's src/main (their src/main is ignored), first as is
# (every test must pass), then against planted bugs (mutants), each of which some test must catch.
#
#   suite_prepare <app-dir> [project]    copy <app-dir> (pom.xml, src/main) and <project>/src/test into $SUITE
#                                        (project defaults to $LAB_DIR)
#   suite_baseline                       mvn test on the unchanged app: passes when every test passes
#   suite_mutants <mutants.json> [ids]   one run per mutant; pass per mutant killed (some test fails)
#   suite_tests_using <regex> <label>    passes when a file under the learner's src/test matches <regex>
# SUITE_GOAL (default "test") is the Maven goal each run uses; "verify" also runs failsafe's *IT classes.
M13=/course/modules/m13-testing/contract
SUITE=; SUITE_APP=; SUITE_KILLED=0; SUITE_TOTAL=0; SUITES=(); SUITE_GOAL="${SUITE_GOAL:-test}"
suite_prepare() { # $1 app dir, $2 the learner's project dir (default $LAB_DIR)
  local mine="${2:-$LAB_DIR}"
  SUITE_APP="$1"; SUITE=$(mktemp -d); SUITES+=("$SUITE"); trap 'rm -rf "${SUITES[@]}"' EXIT
  cp "$SUITE_APP/pom.xml" "$SUITE/" && cp -r "$SUITE_APP/src" "$SUITE/" && rm -rf "$SUITE/src/test"
  if [[ -d "$mine/src/test" ]]; then cp -r "$mine/src/test" "$SUITE/src/test"; else fail "no src/test in ${mine#$LAB_DIR/}"; return 1; fi
  [[ -d "$mine/src/main" ]] && ! diff -rq "$SUITE_APP/src/main" "$mine/src/main" >/dev/null 2>&1 \
    && note "your src/main differs from the contract app's; the grader uses the contract app's src/main"
  return 0
}
# Names of failing tests from the last run, "Class.method", at most 3.
suite_failures() {
  grep -l -E '<(failure|error)' "$SUITE"/target/{surefire,failsafe}-reports/TEST-*.xml 2>/dev/null | while read -r f; do
    python3 - "$f" <<'PY'
import sys, xml.etree.ElementTree as ET
for tc in ET.parse(sys.argv[1]).getroot().iter("testcase"):
    if tc.find("failure") is not None or tc.find("error") is not None:
        print(tc.get("classname").rsplit(".", 1)[-1] + "." + tc.get("name").split("(")[0])
PY
  done | sort -u | head -3 | paste -sd, | sed 's/,/, /g'
}
suite_baseline() {
  if (cd "$SUITE" && mvn -q $SUITE_GOAL >"$SUITE/baseline.log" 2>&1); then
    local n; n=$(cat "$SUITE"/target/{surefire,failsafe}-reports/TEST-*.xml 2>/dev/null | grep -o '<testcase ' | wc -l)
    (( n > 0 )) || { fail "no tests ran (${SUITE_GOAL}): add some"; return 1; }
    pass "your tests pass on the unchanged app ($n tests)"; return 0
  fi
  if grep -q "COMPILATION ERROR" "$SUITE/baseline.log"; then
    fail "your tests do not compile" "$(grep -E '\.java:\[' "$SUITE/baseline.log" | head -3 | sed 's#.*/src/#src/#')"
  else fail "your tests must pass on the unchanged app first" "failing: $(suite_failures)"; fi
  return 1
}
suite_mutants() { # $1 mutants.json, rest: ids (default all)
  local file="$1" id what ids; shift
  ids=("$@"); [[ ${#ids[@]} -eq 0 ]] && mapfile -t ids < <(jq -r '.[].id' "$file")
  for id in "${ids[@]}"; do
    rm -rf "$SUITE/src/main" && cp -r "$SUITE_APP/src/main" "$SUITE/src/main"
    what=$(python3 $M13/mutate.py "$file" "$id" "$SUITE") || { fail "mutant $id does not apply (course bug)"; continue; }
    SUITE_TOTAL=$((SUITE_TOTAL + 1))
    if (cd "$SUITE" && mvn -q -o $SUITE_GOAL >"$SUITE/$id.log" 2>&1); then
      fail "$id survived: $what" "no test failed: which test would notice?"
    elif grep -q "COMPILATION ERROR" "$SUITE/$id.log"; then
      fail "$id: the app does not compile with this mutant (course bug)" "$(grep -E '\.java:\[' "$SUITE/$id.log" | head -2)"
    else
      SUITE_KILLED=$((SUITE_KILLED + 1)); pass "$id killed: $what  [$(suite_failures)]"
    fi
  done
  rm -rf "$SUITE/src/main" && cp -r "$SUITE_APP/src/main" "$SUITE/src/main"
}
suite_tests_using() { # $1 regex (grep -E), $2 label
  if grep -rqE "$1" "$LAB_DIR/src/test" 2>/dev/null; then pass "$2"; else fail "$2"; fi
}

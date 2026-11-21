#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java m09_11 >/dev/null || { fail "could not load the club data into m09_11"; finish; }
# 1. Your own budgets pass.
if mvn -q test >/tmp/own-tests.log 2>&1; then
  n=$(grep -rhoE '@MaxStatements\(' src/test/java --include=*Test.java 2>/dev/null | wc -l)
  (( n >= 3 )) && pass "your tests pass, with $n @MaxStatements budgets" || fail "your tests pass, with at least 3 @MaxStatements budgets" "found $n"
else fail "your tests pass (mvn test)" "$(grep -m3 -E 'FAIL|ERROR\]' /tmp/own-tests.log)"; fi
# 2. The harness's fixtures, run against your extension in a copy of your project.
work=/tmp/n1-detector; rm -rf $work; mkdir -p $work; cp -r pom.xml src ci $work/
mkdir -p $work/src/test/java/dev/shelfie/perf/fixtures; cp "$LAB_HOME"/tests/fixtures/*.java $work/src/test/java/dev/shelfie/perf/fixtures/
(cd $work && mvn -q test -Dtest='*Fixtures*' -Dsurefire.failIfNoSpecifiedTests=false >/tmp/fixtures.log 2>&1)
if compgen -G "$work/target/surefire-reports/TEST-*Fixtures*.xml" >/dev/null; then
  while IFS='|' read -r status name detail; do
    [[ $status == ok ]] && pass "fixture $name" || fail "fixture $name" "$detail"
  done < <(python3 "$LAB_HOME/tests/Verdicts.py" $work/target/surefire-reports "$LAB_HOME/tests/fixtures/expected.txt")
else fail "the fixtures compiled against your extension" "$(grep -m3 -E 'ERROR.*java|cannot find symbol' /tmp/fixtures.log)"; fi
rm -rf $work
# 3. CI runs it.
wf=$(ls .github/workflows/*.yml .github/workflows/*.yaml 2>/dev/null | head -1)
if [[ -n "$wf" ]] && grep -qE '^\s*services:' "$wf" && grep -qE 'postgres' "$wf" && grep -qE 'mvn .*test' "$wf" && grep -q 'ClubData' "$wf"
then pass "a CI workflow runs the budgets against PostgreSQL ($wf)"
else fail "a CI workflow runs the budgets against PostgreSQL" "a workflow in .github/workflows with a postgres service, the data load and mvn test"; fi
finish

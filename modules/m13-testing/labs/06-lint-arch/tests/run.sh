#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
M13A=/course/modules/m13-testing/contract
require_file eslint.config.mjs "graphql-eslint's flat config at the project root" || finish
require_file package.json || finish
grep -q 'spotless-maven-plugin' pom.xml && pass "pom.xml has the Spotless plugin" || fail "pom.xml has the Spotless plugin"
grep -rqE '^\s*@(ArchTest|AnalyzeClasses)' src/test 2>/dev/null && pass "ArchUnit rules exist in src/test" || fail "ArchUnit rules exist in src/test"
if [[ ! -d node_modules ]]; then
  [[ -f package-lock.json ]] && npm ci --silent >/tmp/npm.log 2>&1 || npm install --silent >/tmp/npm.log 2>&1 \
    || { fail "npm could not install the lint tools" "$(tail -3 /tmp/npm.log)"; finish; }
fi
W=$(mktemp -d); trap 'rm -rf $W' EXIT
copy() { rm -rf $W/p && mkdir -p $W/p && cp -r pom.xml src package.json eslint.config.mjs $W/p/ && ln -s "$LAB_DIR/node_modules" $W/p/node_modules; }
# Each tool on a copy: "caught" when it fails. archunit = the tests (mvn test), spotless = mvn spotless:check.
tool() {
  case "$1" in
    archunit) (cd $W/p && mvn -q -o test >$W/$1.log 2>&1 || mvn -q test >$W/$1.log 2>&1) ;;
    spotless) (cd $W/p && mvn -q -o spotless:check >$W/$1.log 2>&1 || mvn -q spotless:check >$W/$1.log 2>&1) ;;
    eslint) (cd $W/p && node node_modules/eslint/bin/eslint.js src/main/resources/graphql >$W/$1.log 2>&1) ;;
  esac
}
copy
for t in archunit spotless eslint; do
  if tool $t; then pass "clean project: $t passes"
  else fail "clean project: $t passes (it must, before it can catch anything)" "$(grep -vE '^\s*$' $W/$t.log | grep -E 'ERROR|error|Violation|was violated' | head -3 | paste -sd'|')"; fi
done
table=()
for id in $(jq -r '.[].id' $LAB_HOME/tests/violations.json); do
  copy
  want=$(jq -r --arg i $id '.[] | select(.id == $i) | .tool' $LAB_HOME/tests/violations.json)
  what=$(python3 $M13A/mutate.py $LAB_HOME/tests/violations.json $id $W/p) || { fail "$id does not apply to your copy" "keep $(jq -r --arg i $id '.[] | select(.id == $i) | .file' $LAB_HOME/tests/violations.json) as in the starter"; continue; }
  caught=()
  for t in archunit spotless eslint; do tool $t || caught+=($t); done
  table+=("$id ${caught[*]:-nothing}")
  [[ " ${caught[*]} " == *" $want "* ]] && pass "$id caught by $want: $what" || fail "$id not caught by $want: $what" "caught by: ${caught[*]:-nothing}"
done
note "who caught what: $(IFS=';'; echo "${table[*]}")"
notes_check notes.md 60 m13/06
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m13-testing/contract/suite-test.sh
cd "$LAB_DIR" || exit 1
M13A=/course/modules/m13-testing/contract
M5=/course/modules/m05-graphql-java/labs/09-project-tiny-server

# Part A: integration tests on Testcontainers (mvn verify runs *IT with failsafe), then three planted bugs.
suite_tests_using '^\s*@Testcontainers' "an integration test uses Testcontainers"
for c in PostgreSQLContainer KafkaContainer KeycloakContainer; do suite_tests_using "new $c\\(" "it starts a $c"; done
ls src/test/java/dev/shelfie/it/*IT.java >/dev/null 2>&1 && pass "integration tests are named *IT (failsafe runs them in mvn verify)" \
  || fail "integration tests are named *IT (failsafe runs them in mvn verify)"
SUITE_GOAL=verify
if suite_prepare $M13A/shelfie-it && suite_baseline; then suite_mutants "$LAB_HOME/tests/mutants.json"; fi

# Part B: a black-box contract (contract.hurl) that both the Module 5 and the Module 13 servers pass, and that
# catches three bugs planted in the Module 13 server. Plus, for the notes, the HTTP audit on both.
t=$(grep -cE '^# C[0-9]+' contract.hurl 2>/dev/null); t=${t:-0}
(( t >= 8 )) && pass "contract.hurl has $t entries (# Cnn)" || fail "contract.hurl needs at least 8 entries titled # C01, # C02, ... (found $t)"
B=$(mktemp -d); SUITES+=("$B")
cp -r $M5/solution/. $B/m05 && (cd $B/m05 && mvn -q package >/dev/null 2>&1) || { fail "could not build the Module 5 server (course bug)"; finish; }
cp -r $M13A/shelfie-app/{pom.xml,src} $B/ 2>/dev/null; mkdir -p $B/m13 && mv $B/pom.xml $B/src $B/m13/
(cd $B/m13 && mvn -q -o package -DskipTests >/dev/null 2>&1) || (cd $B/m13 && mvn -q package -DskipTests >/dev/null 2>&1) \
  || { fail "could not build the Module 13 server (course bug)"; finish; }
contract_on() { # $1 label, rest: the server command; runs shelfie.hurl's S01-S14 and contract.hurl
  local label="$1"; shift
  app_start "$@" || return 1
  app_hurl /course/modules/m06-spring/contract/shelfie.hurl "$label: shelfie.hurl"
  app_hurl "$LAB_DIR/contract.hurl" "$label: contract.hurl"
  local a=$M5/tests/audit.hurl at af
  hurl --test --continue-on-error --variable "url=$URL" $a >/tmp/audit.log 2>&1
  at=$(grep -cE '^# A[0-9]+' $a)
  af=$(grep -oE "audit.hurl:[0-9]+" /tmp/audit.log | cut -d: -f2 | sort -un | while read -r n; do
    awk -v n="$n" 'NR<=n && /^# A[0-9]/ {l=$1" "$2} END {print l}' $a; done | sort -u | grep -c .)
  note "$label: Module 5's HTTP audit (protocol, not behaviour): $((at - af)) of $at"
  app_stop
}
contract_on "Module 5 server" java -jar $B/m05/target/shelfie-server.jar $PORT
contract_on "Module 13 server" java -jar $B/m13/target/shelfie-app.jar
for id in B1 B2 B3; do
  rm -rf $B/mut && mkdir -p $B/mut && cp -r $B/m13/pom.xml $B/m13/src $B/mut/
  what=$(python3 $M13A/mutate.py "$LAB_HOME/tests/blackbox-mutants.json" $id $B/mut) || { fail "$id does not apply (course bug)"; continue; }
  (cd $B/mut && mvn -q -o package -DskipTests >/dev/null 2>&1) || { fail "$id: build failed (course bug)"; continue; }
  app_start java -jar $B/mut/target/shelfie-app.jar || continue
  if hurl --test --continue-on-error --variable "url=$URL" "$LAB_DIR/contract.hurl" >/tmp/mut.log 2>&1; then
    fail "$id survived contract.hurl: $what"
  else pass "$id caught by contract.hurl: $what"; fi
  app_stop
done
notes_check notes.md 60 m13/03
finish

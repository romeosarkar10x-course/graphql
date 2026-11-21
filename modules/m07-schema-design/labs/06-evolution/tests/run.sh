#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
KEY=2d8ae233425c842a1386fbed5eeaa845519a531f6031f81d668905b018d69b14   # sha256 of the SchemaDiff column, space-separated

# Part A: the 15 changes
require_file classify.md || finish
rows=$(grep -E '^\|\s*[0-9]{2}\s*\|' classify.md)
col() { awk -F'|' -v c="$1" '{gsub(/[ \t`*]/, "", $c); print toupper($c)}' <<<"$rows"; }
if (( $(wc -l <<<"$rows") != 15 )); then fail "classify.md should keep its 15 rows"
else
  bad=$(col 4 | grep -cvxE 'BREAKING|DANGEROUS|SAFE')
  (( bad == 0 )) && pass "classify.md: a guess for all 15 changes" || fail "classify.md: $bad guesses missing or not BREAKING/DANGEROUS/SAFE"
  if [[ $(col 5 | paste -sd' ' | tr -d '\n' | sha256sum | cut -c1-64) == "$KEY" ]]; then pass "classify.md: the SchemaDiff column matches graphql-java 26.1"
  else fail "classify.md: the SchemaDiff column does not match what bin/course design diff reports"; fi
  bad=$(col 6 | grep -cvxE 'BREAKING|DANGEROUS|SAFE')
  (( bad == 0 )) && pass "classify.md: the inspector column is filled" || fail "classify.md: $bad inspector results missing (bin/course inspector diff, on the host)"
  right=$(paste -d' ' <(col 4) <(col 5) | awk '$1==$2' | wc -l)
  note "your guesses matched SchemaDiff on $right of 15"
fi

# Part B: the gate
require_file schema-gate/pom.xml || finish
coords=$(java -cp "$(lib_cp)" /course/tools/gql/Gql.java coordinates schema-gate/src/main/resources/schema.graphqls Club.memberCount Club.about CreateReviewInput.spoiler 2>&1)
if [[ $? -eq 0 ]] && grep -A0 -E '^\s*description:.*@deprecated' schema-gate/src/main/resources/schema.graphqls >/dev/null; then
  pass "the three changes shipped: Club.memberCount, Club.about with description deprecated, CreateReviewInput.spoiler"
else fail "ship the README's three changes in schema-gate/src/main/resources/schema.graphqls"; grep missing <<<"$coords" | head -3 | sed 's/^/        /'; fi
gate() { # $1 label, $2 expected: pass|fail, $3 schema file to put in place (optional), $4 released file (optional)
  rm -rf /tmp/gate && cp -r schema-gate /tmp/gate && rm -rf /tmp/gate/target
  [[ -n "${3:-}" ]] && cp "$3" /tmp/gate/src/main/resources/schema.graphqls
  [[ -n "${4:-}" ]] && cp "$4" /tmp/gate/released/schema.graphqls
  if (cd /tmp/gate && mvn -q -o test >/tmp/gate.log 2>&1); then got=pass; else got=fail; fi
  if [[ $got == "$2" ]]; then pass "gate: $1"
  else fail "gate: $1 (expected the build to $2, it did $got)"; grep -E 'ERROR\]|error:' /tmp/gate.log | head -4 | sed 's/^/        /'; fi
}
gate "your schema passes" pass
gate "change 02 (remove a field) fails the build" fail "$LAB_HOME/starter/changes/02.graphqls"
gate "change 05 (an argument becomes required) fails the build" fail "$LAB_HOME/starter/changes/05.graphqls"
gate "change 09 (an output field becomes nullable) fails the build" fail "$LAB_HOME/starter/changes/09.graphqls"
gate "change 04 (a dangerous enum value) passes the build" pass "$LAB_HOME/starter/changes/04.graphqls"
printf 'type Query { a: String @deprecated(reason: "gone") }\n' > /tmp/only-deprecated.graphqls
gate "deprecating a type's only field fails the build (RFC #1231)" fail /tmp/only-deprecated.graphqls /tmp/only-deprecated.graphqls
wf=$(grep -vE '^\s*#' .github/workflows/schema.yml 2>/dev/null)
if grep -q 'graphql-inspector' <<<"$wf" && grep -q 'mvn' <<<"$wf"; then pass ".github/workflows/schema.yml runs mvn test and graphql-inspector"
else fail ".github/workflows/schema.yml should run the JUnit gate (mvn) and graphql-inspector"; fi
notes_check notes.md 120 m07/06
finish

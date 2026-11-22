#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
ref_up || { fail "the reference server is not up: run bin/course ref up"; finish; }
app_build target/shelfie-typed.jar || finish
T=$(mktemp -d); trap 'rm -rf $T' EXIT
if grep -rqE '\.document(Name)?\(|"(query|mutation)? *\{' src/main/java; then
  fail "no hand-written document text in src/main/java (requests and projections are generated)" "$(grep -rnE '\.document(Name)?\(|"(query|mutation)? *\{' src/main/java | head -3)"
else pass "no hand-written document text in src/main/java (requests and projections are generated)"; fi
grep -rq DgsGraphQlClient src/main/java && pass "uses DgsGraphQlClient" || fail "uses DgsGraphQlClient"
for c in c1 c2; do
  ref_reset_stats
  java -jar target/shelfie-typed.jar "$REF/graphql" $c > $T/$c.txt 2> $T/$c.err
  n=$(ref_stats | jq .graphql)
  if diff -q $T/$c.txt "$LAB_HOME/tests/expected-$c.txt" >/dev/null; then pass "club page for $c matches"
  else fail "club page for $c matches" "$(diff $T/$c.txt "$LAB_HOME/tests/expected-$c.txt" | head -6 | paste -sd'|')"; fi
  [[ $n == 1 ]] && pass "club page for $c is one HTTP request (two root fields, one document)" || fail "club page for $c is one HTTP request (two root fields, one document)" "Shelfie counted $n"
done
java -jar target/shelfie-typed.jar "$REF/graphql" nope > $T/nope.txt 2>&1
[[ "$(cat $T/nope.txt)" == "No club nope" ]] && pass "an unknown club prints \"No club nope\"" || fail "an unknown club prints \"No club nope\"" "got $(head -c 200 $T/nope.txt)"
# The experiment: rename Book.title in the schema; the client must stop compiling, in ClubPage.java.
cp -r "$LAB_DIR" $T/renamed && rm -rf $T/renamed/target
schema=$(ls $T/renamed/src/main/resources/schema/*.graphqls | head -1)
awk '/^type Book /{b=1} b && /^}/{b=0} b && /^  title: /{sub(/title:/,"name:")} {print}' "$schema" > $T/s && cp $T/s "$schema"
grep -q '^  name: String!' "$schema" || fail "the test could not rename Book.title in $schema"
if (cd $T/renamed && mvn -q compile > $T/compile.log 2>&1); then
  fail "renaming Book.title to Book.name in the schema breaks the build" "mvn compile still succeeded"
elif grep -qE 'ClubPage\.java.*(cannot find symbol|title)|symbol: +method (title|getTitle)' $T/compile.log; then
  pass "renaming Book.title to Book.name in the schema breaks the build, at compile time"
else fail "renaming Book.title to Book.name in the schema breaks the build, at compile time" "$(grep ERROR $T/compile.log | head -3)"; fi
notes_check notes.md 60 m12/03
finish

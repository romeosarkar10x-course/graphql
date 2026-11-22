#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
ref_up || { fail "the reference server is not up: run bin/course ref up"; finish; }
app_build target/shelfie-cli.jar || finish
T=$(mktemp -d); trap 'rm -rf $T' EXIT
cli() { timeout 60 java -jar target/shelfie-cli.jar "$REF/graphql" "$@"; }
out=$(cli -e '{ hello }' 2>$T/err); code=$?
[[ $code == 0 && $(jq -r .hello <<<"$out" 2>/dev/null) == "Hello from Shelfie!" ]] && pass "-e '{ hello }' prints the data as JSON, exit 0" \
  || fail "-e '{ hello }' prints the data as JSON, exit 0" "exit $code, got: $(head -c 200 <<<"$out")"
cli -e '{ nope }' > /dev/null 2>$T/err; code=$?
[[ $code == 1 ]] && grep -q "Field 'nope'" $T/err && pass "-e with a bad document: the error on stderr, exit 1" \
  || fail "-e with a bad document: the error on stderr, exit 1" "exit $code, stderr: $(head -c 200 $T/err)"
grep -rqE 'implements +(org\.jline\.reader\.)?Completer' src/main/java && pass "a JLine Completer" || fail "a JLine Completer (implements org.jline.reader.Completer)"
ref_reset_stats
ok=0; total=0
while IFS=$'\t' read -r buffer want; do
  total=$((total + 1))
  have=$(cli --complete "$buffer" 2>/dev/null | paste -sd' ')
  if [[ "$have" == "$want" ]]; then ok=$((ok + 1)); else fail "complete \"$buffer\"" "want: ${want:-(nothing)}   have: ${have:-(nothing)}"; fi
done < "$LAB_HOME/tests/completions.tsv"
(( ok == total )) && pass "completion: all $total cases (fields by type, arguments, fragments, keywords)"
n=$(ref_stats | jq .graphql)
[[ $n == $total ]] && pass "each --complete run asked Shelfie once (introspection), $n requests for $total runs" \
  || fail "each --complete run asked Shelfie once (introspection)" "$n requests for $total runs"
printf '{ book(id: "b1") {\n  title\n  authors\n}}\n:schema Club\n:quit\n' | cli > $T/piped 2>&1
grep -q '"title" : "The Left Hand of Darkness"\|"title": "The Left Hand of Darkness"' $T/piped && pass "interactive: a document typed over three lines runs when its braces balance" \
  || fail "interactive: a document typed over three lines runs when its braces balance" "$(tr -d '\r' < $T/piped | tail -5 | paste -sd'|')"
grep -qE 'memberCount: Int!' $T/piped && pass "interactive: :schema Club lists its fields with types" \
  || fail "interactive: :schema Club lists its fields with types (memberCount: Int!)" "$(tr -d '\r' < $T/piped | tail -5 | paste -sd'|')"
# A real terminal: Tab after "ti" must complete to title.
(sleep 8; printf '{ book(id: "b2") { ti\t }}\r'; sleep 3; printf ':quit\r'; sleep 1) \
  | script -qfc "java -jar target/shelfie-cli.jar $REF/graphql" /dev/null > $T/tty 2>&1
grep -q '"title" : "Piranesi"\|"title": "Piranesi"' $T/tty && pass "in a terminal, Tab completes \"ti\" to title and the document runs" \
  || fail "in a terminal, Tab completes \"ti\" to title and the document runs" "$(tr -d '\r' < $T/tty | tail -4 | paste -sd'|' | cut -c1-200)"
finish

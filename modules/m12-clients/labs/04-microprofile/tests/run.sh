#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m12-clients/contract/clients-test.sh
cd "$LAB_DIR" || exit 1
ref_up || { fail "the reference server is not up: run bin/course ref up"; finish; }
app_build target/quarkus-app/quarkus-run.jar || finish
T=$(mktemp -d); trap 'capture_stop; rm -rf $T' EXIT
grep -rq '@GraphQLClientApi' src/main/java && pass "a typesafe client interface (@GraphQLClientApi)" || fail "a typesafe client interface (@GraphQLClientApi)"
grep -rq 'DynamicGraphQLClient' src/main/java && pass "a dynamic client (DynamicGraphQLClient)" || fail "a dynamic client (DynamicGraphQLClient)"
run() { java -Dquarkus.smallrye-graphql-client.shelfie.url=${URL_OVERRIDE:-$REF/graphql} -jar target/quarkus-app/quarkus-run.jar "$@" 2> $T/err; }
for mode in typesafe dynamic; do
  for c in c1 c2; do
    run $mode $c > $T/$mode-$c.txt
    if diff -q $T/$mode-$c.txt "$LAB_HOME/tests/expected-$c.txt" >/dev/null; then pass "$mode: club card for $c matches"
    else fail "$mode: club card for $c matches" "$(diff $T/$mode-$c.txt "$LAB_HOME/tests/expected-$c.txt" | head -6 | paste -sd'|') $(grep -m1 -E 'Exception|ERROR' $T/err)"; fi
  done
  [[ "$(run $mode nope)" == "No club nope" ]] && pass "$mode: an unknown club prints \"No club nope\"" || fail "$mode: an unknown club prints \"No club nope\"" "$(grep -m1 -E 'Exception|ERROR' $T/err)"
done
# On the wire: both clients send the club id as a variable, and reviews(last: 2) on the nested field.
capture_start 4999 $T/wire || { fail "the capture proxy did not start"; finish; }
URL_OVERRIDE=http://localhost:4999/graphql
for mode in typesafe dynamic; do
  before=$(wc -l < $T/wire); run $mode c1 > /dev/null; sleep 0.3
  body=$(sed -n "$((before + 1))s/^POST //p" $T/wire); sent=$(( $(wc -l < $T/wire) - before ))
  if [[ $sent == 1 && $(jq -r '.variables.id // empty' <<<"$body" 2>/dev/null) == c1 ]] && ! grep -q '"c1\\"' <<<"$body" && grep -qE 'reviews\(last: ?(2|\$[a-zA-Z]+)\)' <<<"$body"; then
    pass "$mode: one request, the id as a variable, reviews(last: 2) asked of the server"
  else fail "$mode: one request, the id as a variable, reviews(last: 2) asked of the server" "sent: $(head -c 300 <<<"$body")"; fi
done
capture_stop
notes_check notes.md 60 m12/04
finish

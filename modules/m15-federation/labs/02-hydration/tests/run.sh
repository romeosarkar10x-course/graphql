#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-nadel.jar
W=$(mktemp -d); PIDS=(); trap 'kill ${PIDS[@]} 2>/dev/null; rm -rf $W; app_stop' EXIT
app_build $JAR || finish
P=/course/modules/m15-federation/contract/plain
(cd $P && exec java -cp "$(lib_cp)" PlainService.java catalog 4101) > /tmp/plain-catalog.log 2>&1 & PIDS+=($!)
(cd $P && exec java -cp "$(lib_cp)" PlainService.java reviews 4102) > /tmp/plain-reviews.log 2>&1 & PIDS+=($!)
for p in 4101 4102; do for _ in $(seq 60); do curl -fs -o /dev/null localhost:$p/_stats && break; sleep 0.5; done; done
URL=http://localhost:4000/graphql
app_start java -jar $JAR || finish
app_hurl $LAB_HOME/tests/hydration.hurl "the gateway: hydration and batching (hydration.hurl)" \
  --variable catalog=http://localhost:4101 --variable reviews=http://localhost:4102
app_stop
# The experiment, by the grader: rename Book.title to Book.name at the gateway, from your own schema files.
cp -r src/main/resources $W/schemas
sed -i -E 's/^(\s*)title: String!/\1name: String! @renamed(from: "title")/' $W/schemas/nadel/catalog.nadel
app_start java -jar $JAR --schemas=$W/schemas || finish
q() { jq -n --arg q "$1" '{query: $q}' | curl -s $URL -H 'Content-Type: application/json' --data-binary @-; }
r1=$(q '{ latestReviews(first: 2) { book { name } } }'); r2=$(q '{ book(id: "b1") { title } }')
if [[ $(jq -r '.data.latestReviews[0].book.name' <<<"$r1") == "Station Eleven" ]] && [[ $(jq '.errors | length' <<<"$r2") -gt 0 ]]; then
  pass "experiment: --schemas with Book.title renamed to name at the gateway: name works, title is gone"
else fail "experiment: rename at the gateway with --schemas=<dir>" "name: $(cut -c1-150 <<<"$r1") / title: $(cut -c1-150 <<<"$r2")"; fi
notes_check notes.md 80 m15/02
finish

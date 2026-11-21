#!/usr/bin/env bash
source /course/tools/testlib.sh
source "$LAB_HOME/../../contract/app-test.sh"
cd "$LAB_DIR" || exit 1
app_build target/shelfie-boot.jar || finish
app_start java -jar target/shelfie-boot.jar || finish
pass "your app starts"
app_hurl "$LAB_HOME/../../contract/shelfie.hurl" "Shelfie on Spring (shelfie.hurl)"
curl -s -H 'Content-Type: application/json' -d '{"query":"{ books(first: null) { id } club(id: \"c1\") { members(first: null) { id } } }"}' "$URL" >/tmp/nulls.json
jq -e '(.errors | not) and (.data.books | length) == 10 and (.data.club.members | length) == 8' /tmp/nulls.json >/dev/null \
  && pass "explicit null arguments do not crash (books 10, members 8)" || fail "books(first: null) and members(first: null) should fall back to the defaults" "got: $(cut -c1-160 /tmp/nulls.json)"
app_stop
# Three mismatches in a copy: an unmapped field, a renamed method, a nullability lie.
rm -rf /tmp/mut && cp -r "$LAB_DIR" /tmp/mut && cd /tmp/mut && rm -rf target
sed -i 's/^  memberCount: Int!$/  memberCount: Int!\n  tagline: String/; s/^  currentBook: Book$/  currentBook: Book!/' src/main/resources/graphql/shelfie.graphqls
grep -rl 'memberCount(' src/main/java | xargs -r sed -i 's/\bmemberCount(/memberTotal(/'
if ! mvn -q package -DskipTests >/tmp/mut-build.log 2>&1; then fail "the mutated copy should still compile" "$(grep ERROR /tmp/mut-build.log | head -2)"; finish; fi
java -jar target/shelfie-boot.jar >/tmp/mut.log 2>&1 & MPID=$!
for _ in $(seq 1 60); do kill -0 $MPID 2>/dev/null || break; listening && break; sleep 0.5; done
if listening; then fail "with 3 mismatches the app still started: make the inspection fail startup"; kill $MPID; wait $MPID 2>/dev/null
else
  pass "with 3 mismatches the app refuses to start"
  for w in tagline memberCount currentBook; do
    grep -q "$w" /tmp/mut.log && pass "the failure names $w" || fail "the startup failure should name $w (did you annotate nullability?)"
  done
fi
cd "$LAB_DIR"; notes_check notes.md 60 m06/03
finish

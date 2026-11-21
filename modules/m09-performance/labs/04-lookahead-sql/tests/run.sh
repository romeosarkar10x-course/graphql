#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java m09_04 >/dev/null || { fail "could not load the club data into m09_04"; finish; }
jq -n --rawfile q /course/modules/m09-performance/contract/dashboard.graphql '{query: $q}' > /tmp/dashboard.json
app_build target/shelfie-perf.jar || finish
app_start java -jar target/shelfie-perf.jar || finish
app_hurl "$LAB_HOME/tests/lookahead.hurl" "lookahead (lookahead.hurl)" --file-root /tmp
got=$(curl -s -H 'Content-Type: application/json' -d @/tmp/dashboard.json "$URL" | jq -cS 'del(.extensions)' | md5sum)
[[ "$got" == "$(jq -cS . /course/modules/m09-performance/contract/dashboard.expected.json | md5sum)" ]] \
  && pass "the dashboard data is the same as the naive version's" || fail "the dashboard data is the same as the naive version's" "compare with contract/dashboard.expected.json"
# Selection-driven: asking only for names must not touch the review or membership tables.
: > /tmp/app.log.mark; before=$(wc -l < /tmp/app.log)
curl -s -o /dev/null -H 'Content-Type: application/json' -d '{"query":"{ clubs(first: 3) { name } }"}' "$URL"
sleep 0.5
tail -n +$((before + 1)) /tmp/app.log > /tmp/names.log
if grep -qiE 'select' /tmp/names.log && ! grep -qiE '"?(review|membership)"?' /tmp/names.log
then pass "clubs { name } reads only the club table"
else fail "clubs { name } reads only the club table" "$(grep -m1 -oiE '.{0,40}(review|membership).{0,20}' /tmp/names.log)"; fi
app_stop
notes_check notes.md 60 m09/04
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java m09_01 >/dev/null || { fail "could not load the club data into m09_01"; finish; }
jq -n --rawfile q /course/modules/m09-performance/contract/dashboard.graphql '{query: $q}' > /tmp/dashboard.json
app_build target/shelfie-perf.jar || finish
app_start java -jar target/shelfie-perf.jar || finish
app_hurl "$LAB_HOME/tests/count.hurl" "statement counts (count.hurl)" --file-root /tmp
# Counting must not change the answers.
got=$(curl -s -H 'Content-Type: application/json' -d @/tmp/dashboard.json "$URL" | jq -cS 'del(.extensions)' | md5sum)
[[ "$got" == "$(jq -cS . /course/modules/m09-performance/contract/dashboard.expected.json | md5sum)" ]] \
  && pass "the dashboard data is unchanged" || fail "the dashboard data is unchanged" "compare with contract/dashboard.expected.json"
app_stop
lines=$(grep -c '^Hibernate: ' /tmp/app.log)
(( lines > 1250 )) && pass "Hibernate SQL logging is on ($lines statements in the log)" || fail "Hibernate SQL logging is on" "set spring.jpa.show-sql: true"
notes_check notes.md 60 m09/01
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java m09_03 >/dev/null || { fail "could not load the club data into m09_03"; finish; }
jq -n --rawfile q /course/modules/m09-performance/contract/dashboard.graphql '{query: $q}' > /tmp/dashboard.json
app_build target/shelfie-perf.jar || finish
app_start java -jar target/shelfie-perf.jar || finish
# A chained loader that is never dispatched hangs the request: give it 10 seconds.
code=$(curl -s -o /dev/null -m 10 -w '%{http_code}' -H 'Content-Type: application/json' -d '{"query":"{ clubs(first: 2) { ownerPicks { title } } }"}' "$URL")
[[ "$code" == 200 ]] && pass "ownerPicks answers (chained loaders are dispatched)" \
  || fail "ownerPicks answers within 10 s" "no response: a load made after the level was dispatched waits forever (lecture 9.3)"
app_hurl "$LAB_HOME/tests/batch.hurl" "batched loading (batch.hurl)" --file-root /tmp
got=$(curl -s -m 10 -H 'Content-Type: application/json' -d @/tmp/dashboard.json "$URL" | jq -cS 'del(.extensions)' | md5sum)
[[ "$got" == "$(jq -cS . /course/modules/m09-performance/contract/dashboard.expected.json | md5sum)" ]] \
  && pass "the dashboard data is the same as the naive version's" || fail "the dashboard data is the same as the naive version's" "compare with contract/dashboard.expected.json"
app_stop
src=$(cat src/main/java/dev/shelfie/perf/*.java)
grep -q '@BatchMapping' <<<"$src" && grep -qE 'BatchLoaderRegistry|registerMappedBatchLoader|registerBatchLoader' <<<"$src" \
  && pass "uses both @BatchMapping and BatchLoaderRegistry" || fail "uses both @BatchMapping and BatchLoaderRegistry" "fields with arguments need a registered loader keyed by the argument"
notes_check notes.md 60 m09/03
finish

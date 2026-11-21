#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java m09_06 >/dev/null || { fail "could not load the club data into m09_06"; finish; }
app_build target/shelfie-perf.jar || finish
app_start java -jar target/shelfie-perf.jar || finish
python3 /course/tools/varnish.py 4000 >/dev/null || { fail "could not point Varnish at the app (bin/course ref up starts it)"; finish; }
app_hurl "$LAB_HOME/tests/apq.hurl" "persisted queries (apq.hurl)" --variable varnish=http://varnish:6081/graphql
# The measurement, with the harness's own copy of Wire.java, on a fresh Varnish.
python3 /course/tools/varnish.py 4000 >/dev/null
if out=$(java "$LAB_HOME/tests/Wire.java" 2>&1); then
  echo "$out" | sed 's/^/        /'
  post=$(awk '$1=="POST"{print $2}' <<<"$out"); apq=$(awk '$1=="APQ"{print $3}' <<<"$out"); rate=$(awk '$1=="APQ"{print $4}' <<<"$out" | tr -d %)
  (( apq < post )) && pass "APQ GET sends fewer bytes than POST ($apq < $post)" || fail "APQ GET sends fewer bytes than POST" "$apq vs $post"
  (( rate >= 90 )) && pass "Varnish serves APQ GETs: hit rate $rate%" || fail "Varnish serves APQ GETs: hit rate $rate% (want 90%+)" "is the PersistedQueryNotFound response cached, or is Cache-Control missing?"
else fail "Wire.java ran" "$(tail -2 <<<"$out")"; fi
notes_check notes.md 80 m09/06
finish

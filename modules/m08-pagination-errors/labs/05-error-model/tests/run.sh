#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
for f in Depths.java predictions.json; do require_file "$f" || finish; done
rm -rf /tmp/depths && mkdir -p /tmp/depths
if javac -cp "$(lib_cp)" -d /tmp/depths Depths.java "$LAB_HOME/tests/ModelCheck.java" 2>/tmp/depths.log; then
  out=$(java -cp "/tmp/depths:$(lib_cp)" ModelCheck 2>&1); status=$?
  if (( status == 0 )); then pass "errors at every depth, and a prediction for each case ($(grep -c '^ok' <<<"$out") checks)"
  else fail "error model: $(grep -c '^FAIL' <<<"$out") check(s) fail"; grep '^FAIL' <<<"$out" | head -12 | sed 's/^FAIL  /        /'; fi
  grep '^info' <<<"$out" | sed 's/^info  /        /'
else fail "Depths.java does not compile with the harness" "$(grep error /tmp/depths.log | head -3)"; fi
notes_check notes.md 60 m08/05
finish

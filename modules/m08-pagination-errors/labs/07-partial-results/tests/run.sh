#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
require_file ClubPage.java || finish
rm -rf /tmp/partial && mkdir -p /tmp/partial
# The harness's own Services.java: the modes and counters must not be edited.
if javac -cp "$(lib_cp)" -d /tmp/partial ClubPage.java "$LAB_HOME/solution/Services.java" "$LAB_HOME/tests/PartialCheck.java" 2>/tmp/partial.log; then
  if out=$(java -cp "/tmp/partial:$(lib_cp)" PartialCheck 2>&1); then pass "partial results in three modes ($(grep -c '^ok' <<<"$out") checks)"
  else fail "partial results: $(grep -c '^FAIL' <<<"$out") check(s) fail"; grep -vE '^ok' <<<"$out" | head -14 | sed 's/^FAIL  /        /'; fi
else fail "ClubPage.java does not compile with the harness" "$(grep error /tmp/partial.log | head -3)"; fi
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
require_file redesign.graphqls || finish
if out=$(java -cp "$(lib_cp)" "$LAB_HOME/tests/Lint.java" redesign.graphqls 2>&1); then pass "redesign.graphqls passes the mutation lint"
else fail "redesign.graphqls breaks lint rules"; grep -E '^fail|Exception|error' <<<"$out" | head -12 | sed 's/^/        /'; fi
for m in createReview updateClub joinClub leaveClub setShelfStatus deleteReviews; do
  grep -qE "^\s*$m\s*\(" redesign.graphqls || fail "redesign.graphqls should have a mutation named $m (README's naming)"
done
rm -rf /tmp/bind && mkdir -p /tmp/bind
if javac -cp "$(lib_cp)" -d /tmp/bind Inputs.java Bind.java "$LAB_HOME/tests/BindCheck.java" 2>/tmp/bind.log; then
  java -cp "/tmp/bind:$(lib_cp)" BindCheck "$LAB_HOME/tests/bind-cases.json" >/tmp/bind.txt 2>&1
  if diff -q /tmp/bind.txt "$LAB_HOME/tests/expected/bind.txt" >/dev/null; then pass "Bind maps all 8 inputs to records (absent, null and set kept apart)"
  else fail "Bind output differs (< yours, > expected)"; diff /tmp/bind.txt "$LAB_HOME/tests/expected/bind.txt" | head -12 | sed 's/^/        /'; fi
else fail "Inputs.java and Bind.java do not compile with the test harness" "$(grep error /tmp/bind.log | head -3)"; fi
notes_check notes.md 60 m04/02
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
require_file Undo.java || finish
rm -rf /tmp/undo && mkdir -p /tmp/undo
javac -cp "$(lib_cp)" -d /tmp/undo Undo.java "$LAB_HOME/tests/Script.java" 2>/tmp/undo.log || { fail "Undo.java does not compile with the test harness (keep Undo.graphQL())" "$(grep error /tmp/undo.log | head -2)"; finish; }
for f in "$LAB_HOME"/tests/scripts/*.json; do
  n=$(basename "$f" .json)
  timeout 60 java -cp "/tmp/undo:$(lib_cp)" Script "$f" >/tmp/undo-$n.txt 2>/tmp/undo-$n.err
  if diff -q /tmp/undo-$n.txt "$LAB_HOME/tests/expected/$n.txt" >/dev/null; then pass "script $n"
  else fail "script $n differs (< yours, > expected; steps are numbered)"; diff /tmp/undo-$n.txt "$LAB_HOME/tests/expected/$n.txt" | grep '^[<>]' | head -6 | cut -c1-170 | sed 's/^/        /'
       grep -m1 -E 'Exception' /tmp/undo-$n.err | sed 's/^/        /'; fi
done
grep -qE 'sealed\s+interface\s+Command' Undo.java && pass "commands are a sealed interface" || fail "model the changes as a sealed Command interface (README)"
notes_check DESIGN.md 60 m04/08
finish

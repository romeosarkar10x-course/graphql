#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
require_file Validation.java || finish
timeout 120 java -cp "$(lib_cp)" Validation.java "$LAB_HOME/tests/cases.jsonl" >/tmp/layers.txt 2>/tmp/layers.err
if diff -q /tmp/layers.txt "$LAB_HOME/tests/expected/layers.txt" >/dev/null; then pass "all 21 inputs stopped at the right layer"
else
  right=$(comm -12 <(sort /tmp/layers.txt) <(sort "$LAB_HOME/tests/expected/layers.txt") | wc -l)
  fail "$right/21 inputs stopped at the expected layer (< yours, > expected)"
  diff /tmp/layers.txt "$LAB_HOME/tests/expected/layers.txt" | grep '^[<>]' | head -14 | sed 's/^/        /'
  grep -E 'Exception|error:' /tmp/layers.err | head -2 | sed 's/^/        /'
fi
grep -qE '@ContainerSize|@Range|@Size|@Pattern' schema.graphqls && pass "schema.graphqls declares its shape rules" || fail "put the shape rules in schema.graphqls as directives"
notes_check notes.md 60 m04/05
finish

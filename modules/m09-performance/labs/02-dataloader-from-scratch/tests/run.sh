#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
out=/tmp/miniloader; rm -rf "$out"; mkdir -p "$out"
if ! javac -d "$out" MiniLoader.java "$LAB_HOME/tests/LoaderSuite.java" 2>/tmp/javac.log; then
  fail "MiniLoader.java compiles against the suite" "$(grep -m3 error /tmp/javac.log)"; finish
fi
pass "MiniLoader.java compiles against the suite"
timeout 60 java -cp "$out" LoaderSuite > /tmp/suite.log 2>&1
while read -r status name; do
  case "$status" in ok) pass "$name" ;; FAIL) fail "${name%%: *}" "${name#*: }" ;; esac
done < <(grep -E '^(ok|FAIL) ' /tmp/suite.log)
grep -qE 'cases (pass|fail)' /tmp/suite.log || fail "the suite finished" "$(tail -3 /tmp/suite.log)"
finish

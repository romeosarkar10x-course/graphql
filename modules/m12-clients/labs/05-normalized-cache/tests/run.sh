#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
C=/course/modules/m12-clients/contract/cache
require_file NormalizedCache.java "your cache" || finish
T=$(mktemp -d); trap 'rm -rf $T' EXIT
cp NormalizedCache.java $C/Scenarios.java $C/LiveDemo.java $T/
report() { while IFS=$'\t' read -r line detail; do
  case "$line" in PASS\ *) pass "${1}${line#PASS }" ;; FAIL\ *) fail "${1}${line#FAIL }" "$detail" ;; esac; done; }
java -cp "$(lib_cp)" $T/Scenarios.java $C/scenarios 01-10 > $T/out 2> $T/err; report "scenario " < $T/out
grep -q . $T/out || fail "the scenarios did not run" "$(grep -E 'error|Exception' $T/err | head -5)"
if ref_up; then
  java -cp "$(lib_cp)" $T/LiveDemo.java $REF > $T/live 2> $T/err; report "live: " < <(grep -E '^(PASS|FAIL)' $T/live)
  grep -qE '^(PASS|FAIL)' $T/live || fail "live: the experiment did not run" "$(grep -E 'error|Exception' $T/err | head -3)"
  ref_reset_data
else fail "live: the reference server is not up (bin/course ref up)"; fi
notes_check notes.md 60 m12/05
finish

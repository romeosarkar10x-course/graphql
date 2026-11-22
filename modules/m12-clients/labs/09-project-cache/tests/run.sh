#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
C=/course/modules/m12-clients/contract/cache
require_file NormalizedCache.java "your cache" || finish
T=$(mktemp -d); trap 'rm -rf $T' EXIT
cp NormalizedCache.java $C/Scenarios.java $T/
java -cp "$(lib_cp)" $T/Scenarios.java $C/scenarios > $T/out 2> $T/err
while IFS=$'\t' read -r line detail; do
  case "$line" in PASS\ *) pass "scenario ${line#PASS }" ;; FAIL\ *) fail "scenario ${line#FAIL }" "$detail" ;; esac
done < $T/out
grep -q . $T/out || fail "the scenarios did not run" "$(grep -E 'error|Exception' $T/err | head -5)"
n=$(grep -c '^PASS' $T/out); note "$n of 25 scenarios pass"
# Group variant: scenarios your friends wrote, in scenarios/ next to your cache (optional, not graded here).
if compgen -G "scenarios/*.json" > /dev/null; then
  java -cp "$(lib_cp)" $T/Scenarios.java scenarios > $T/mine 2>&1
  note "your group's scenarios: $(grep -c '^PASS' $T/mine) pass, $(grep -c '^FAIL' $T/mine) fail (not graded)"
fi
finish

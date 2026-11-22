#!/usr/bin/env bash
# answers.json: the spec section behind each of five behaviours. Wrong ones are named, never corrected.
source /course/tools/testlib.sh
source /course/modules/m17-under-the-hood/contract/answers/answers.sh
cd "$LAB_DIR" || exit 1
if jq -e . answers.json >/dev/null 2>&1; then
  read -r points wrong <<<"$(answers_score answers.json "$LAB_HOME/tests/key.tsv")"
  (( points == 5 )) && pass "answers.json: all 5 sections right" || fail "answers.json: $points of 5 sections right" "not yet: $wrong"
else fail "answers.json is valid JSON"; fi
require_file NOTES.md && notes_check NOTES.md 150 m17/01
finish

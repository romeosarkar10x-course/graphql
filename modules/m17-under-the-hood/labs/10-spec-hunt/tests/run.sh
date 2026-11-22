#!/usr/bin/env bash
# Scores answers.json (and, for the group variant, every answers/<player>.json). Wrong answers are named, never
# corrected; 12 of 15 to pass.
source /course/tools/testlib.sh
source /course/modules/m17-under-the-hood/contract/answers/answers.sh
cd "$LAB_DIR" || exit 1
KEY=$LAB_HOME/tests/key.tsv
if jq -e . answers.json >/dev/null 2>&1; then
  read -r points wrong <<<"$(answers_score answers.json "$KEY")"
  (( points >= 12 )) && pass "answers.json: $points of 15${wrong:+ (not yet: $wrong)}" || fail "answers.json: $points of 15, 12 to pass" "${wrong:+not yet: $wrong}"
else fail "answers.json is valid JSON"; fi
if compgen -G "answers/*.json" >/dev/null; then
  echo "  leaderboard (group variant; ties go to the earlier file time):"
  for f in answers/*.json; do read -r points wrong <<<"$(answers_score "$f" "$KEY")"; printf '%s\t%s\t%s\n' "$points" "$(stat -c %Y "$f")" "$(basename "$f" .json)"; done \
    | sort -t$'\t' -k1,1rn -k2,2n | awk -F'\t' '{printf "        %2d  %s\n", $1, $3}'
fi
finish

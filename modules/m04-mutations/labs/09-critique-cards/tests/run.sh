#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
[[ -s cards.json ]] && jq . cards.json >/dev/null 2>&1 || { fail "cards.json is missing or not JSON"; finish; }
REF="$LAB_HOME/tests/reference.json"
missing=0; short=0
for c in $(jq -r 'keys[]' "$REF"); do
  for k in input payload errors retry naming; do
    v=$(jq -r --arg c "$c" --arg k "$k" '.[$c][$k] // "null"' cards.json)
    [[ "$v" =~ ^[012]$ ]] || missing=$((missing+1))
    w=$(jq -r --arg c "$c" --arg k "$k" '.[$c].why[$k] // ""' cards.json | wc -w)
    (( w >= 5 )) || short=$((short+1))
  done
done
(( missing == 0 )) && pass "all 8 cards scored on 5 criteria (0, 1 or 2)" || fail "$missing score(s) missing or not 0/1/2"
(( short == 0 )) && pass "every score has a reason of 5+ words" || fail "$short score(s) without a real reason (5+ words in why)"
if (( missing == 0 )); then
  dist=$(jq -n --slurpfile mine cards.json --slurpfile ref "$REF" '
    [ $ref[0] | to_entries[] | .key as $c | .value | to_entries[] | (($mine[0][$c][.key]) - .value) | fabs ] | add')
  note "distance from the reference scores: $dist (0 = identical, 80 = opposite); see ${LAB_HOME#/course/}/solution/ after you pass"
fi
notes_check verdict.md 60 m04/09
finish

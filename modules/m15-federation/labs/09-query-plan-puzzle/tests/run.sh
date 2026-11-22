#!/usr/bin/env bash
# Scores answers.json (and, for the group variant, every answers/<player>.json) against Apollo Router's plans.
# Wrong answers are named, never corrected: the plans stay a puzzle.
source /course/tools/testlib.sh
source /course/modules/m15-federation/contract/fed.sh
cd "$LAB_DIR" || exit 1
W=$(mktemp -d); trap 'rm -rf $W; fed_stop' EXIT
compose $FED/compose/supergraph.yaml $W/sg.graphql && subs_start && router_start apollo $W/sg.graphql || { fail "subgraphs and router start"; finish; }
P=$LAB_HOME/tests/puzzles
for q in $P/p*.graphql; do plan_steps apollo http://127.0.0.1:4000/ "$(grep -v '^#' $q)" | jq -c .steps > $W/$(basename $q .graphql).plan; done
score() { # answers file -> "points wrong-list"
  local f=$1 n=0 wrong=() q mine
  for q in $P/p*.graphql; do
    q=$(basename $q .graphql)
    mine=$(jq -c --arg q $q '.[$q] // [] | map(sort)' "$f" 2>/dev/null)
    [[ "$mine" == "$(cat $W/$q.plan)" ]] && n=$((n+1)) || wrong+=($q)
  done
  echo "$n ${wrong[*]}"
}
if [[ -f answers.json ]] && jq -e . answers.json >/dev/null 2>&1; then
  read -r points wrong <<<"$(score answers.json)"
  if (( points >= 7 )); then pass "answers.json: $points of 10 exact${wrong:+ (wrong: $wrong)}"
  else fail "answers.json: $points of 10 exact, 7 to pass" "${wrong:+wrong: $wrong}"; fi
else fail "answers.json is valid JSON"; fi
if compgen -G "answers/*.json" >/dev/null; then
  echo "  leaderboard (group variant):"
  for f in answers/*.json; do read -r points wrong <<<"$(score $f)"; printf '%s\t%s\n' "$points" "$(basename $f .json)"; done \
    | sort -rn | awk -F'\t' '{printf "        %2d  %s\n", $1, $2}'
fi
finish

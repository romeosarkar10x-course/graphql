#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
KEY="$LAB_HOME/tests/key.json"
[[ -s bets.json ]] && jq . bets.json >/dev/null 2>&1 || { fail "bets.json is missing or not JSON"; finish; }
fields=$(jq -r '.winning | keys[]' "$KEY")
ok=1
for f in $fields; do
  bet=$(jq -r --arg f "$f" '.[$f].bet // ""' bets.json); stake=$(jq -r --arg f "$f" '.[$f].stake // 0' bets.json)
  [[ "$bet" == nullable || "$bet" == non-null ]] || { ok=0; fail "$f: bet must be \"nullable\" or \"non-null\""; }
  [[ "$stake" =~ ^[123]$ ]] || { ok=0; fail "$f: stake must be 1, 2 or 3"; }
done
total=$(jq '[.[].stake] | add' bets.json)
[[ "$total" == 16 ]] || { ok=0; fail "stakes must sum to 16 (yours: $total)"; }
(( ok )) || finish
pass "bets.json is a valid slip: 8 bets, 16 points"
score=0; salt=$(jq -r .salt "$KEY")
for f in $fields; do
  bet=$(jq -r --arg f "$f" '.[$f].bet' bets.json); stake=$(jq -r --arg f "$f" '.[$f].stake' bets.json)
  h=$(printf '%s|%s|%s' "$salt" "$f" "$bet" | sha256sum | cut -c1-16)
  if [[ "$h" == "$(jq -r --arg f "$f" '.winning[$f]' "$KEY")" ]]; then score=$((score+stake)); r="+$stake"; else score=$((score-stake)); r="-$stake"; fi
  printf '        %-20s %-9s %3s  %s\n' "$f" "$bet" "$r" "$(jq -r --arg f "$f" '.scenarios[$f]' "$KEY")"
done
(( score >= 8 )) && pass "score $score/16 (pass: 8)" || fail "score $score/16 (pass: 8): read each scenario, then lecture 3.4 again"
n=$(grep -E '^[0-9]\.' reasons.md 2>/dev/null | grep -cvE ':\s*$')
(( n == 8 )) && pass "reasons.md: a reason for every bet (rubric: bin/course grade m03/11)" || fail "reasons.md: one sentence per bet ($n of 8)"
finish

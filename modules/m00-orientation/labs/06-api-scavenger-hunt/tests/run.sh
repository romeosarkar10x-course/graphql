#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file answers.txt || finish
# sha256 of the lower-cased answers; verified against GitHub's public schema on 2026-11-21
declare -A KEY=(
  [Q1]=e2885f2bbe3d7a8b
  [Q2]=6a8c0faa6cea379c
  [Q3]=339563b87e85bd4e
  [Q4]=8fcbe8fde0ff2539
  [Q5]=52c03b546f4afd23
  [Q6]=b3d815b8871abe0d
  [Q7]=93775ee12c16ecf8
  [Q8]=43e3ece73ff8f4eb
)
score=0
for q in Q1 Q2 Q3 Q4 Q5 Q6 Q7 Q8; do
  v=$(grep -E "^$q=" "$LAB_DIR/answers.txt" | head -1 | cut -d= -f2- | tr -d ' ' | tr 'A-Z' 'a-z')
  h=$(printf '%s' "$v" | sha256sum | cut -c1-16)
  if [[ -n "$v" && "$h" == "${KEY[$q]}" ]]; then score=$((score+1)); pass "$q"; else fail "$q ${v:+(\"$v\" is not it)}"; fi
done
note "score: $score/8. group play: first to 8/8 wins; record times in games/leaderboards/api-scavenger-hunt.md"
finish

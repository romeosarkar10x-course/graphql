#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
require_file answers.md || finish
sha() { printf '%s' "$1" | sha256sum | cut -c1-64; }
points=0; report=""
while read -r n kc ko; do
  [[ "$n" == "#" ]] && continue
  line=$(grep -E "^\s*$n\s*:" answers.md | head -1)
  cause=$(sed -E 's/^\s*[0-9]+\s*:\s*([A-Za-z?])\b.*/\1/' <<<"$line" | tr a-z A-Z)
  coord=$(sed -E 's/^\s*[0-9]+\s*:\s*[A-Za-z?]\s+//; s/\s+//g' <<<"$line")
  c=0; o=0
  [[ "$(sha "$n:$cause")" == "$kc" ]] && c=1
  [[ "$(sha "$n:$coord")" == "$ko" ]] && o=1
  points=$((points + c + o))
  report+="incident $n: cause $([[ $c == 1 ]] && echo right || echo wrong), origin $([[ $o == 1 ]] && echo right || echo wrong)"$'\n'
done < "$LAB_HOME/tests/key.txt"
if (( points >= 10 )); then pass "error detective: $points of 12 points"; else fail "error detective: $points of 12 points (10 to pass)"; fi
printf '%s' "$report" | sed 's/^/        /'
finish

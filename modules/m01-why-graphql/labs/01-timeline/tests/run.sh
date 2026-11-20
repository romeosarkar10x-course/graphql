#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file timeline.txt || finish
# sha256 of the correct letter order, so the test does not print the answer
KEY=460e0d5f3ed0be3e
# position of each letter, hashed per position: lets us say how many are in place without saying which order is right
POS=(
  03a51bd6760f
  4978fd031fb4
  b1bd7eac624b
  83daaf967735
  32fdc9be120c
  84f8af883c31
  a1687346ab67
  d8e011af688c
  a403f9b06cd7
  d7771d5f941d
  d0dd75c352d5
  3dfb6c0dea5d
)
got=$(grep -oE '^[A-L] ' "$LAB_DIR/timeline.txt" | tr -d ' \n')
[[ ${#got} -eq 12 && $(fold -w1 <<<"$got" | sort -u | wc -l) -eq 12 ]] || { fail "timeline.txt must keep all 12 lines A-L exactly once"; finish; }
inplace=0
for i in $(seq 0 11); do
  [[ "$(printf '%s%s' "$i" "${got:$i:1}" | sha256sum | cut -c1-12)" == "${POS[$i]}" ]] && inplace=$((inplace+1))
done
if [[ "$(printf '%s' "$got" | sha256sum | cut -c1-16)" == "$KEY" ]]; then pass "all 12 in order"
else fail "$inplace of 12 cards are in the right position"; fi
c="$LAB_DIR/consequences.md"
[[ -s "$c" ]] && (( $(grep -cE '^## [0-9]\. Card [A-L]' "$c") == 4 )) && (( $(wc -w < "$c") > 150 )) \
  && pass "consequences.md: 4 cards written up (rubric: bin/course grade m01/01)" \
  || fail "consequences.md: pick 4 cards (\"## 1. Card X\") and write each up"
finish

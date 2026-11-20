#!/usr/bin/env bash
source /course/tools/testlib.sh
# sha256 (first 16 hex) of the expected data, jq -cS; computed from the reference fixes on 2026-11-21.
declare -A KEY=(
  [01]=79c4824a1eed8827
  [02]=0737b38330232726
  [03]=4bf298f6561839aa
  [04]=78b3e8f5e3d83bf5
  [05]=3336abd7e1c5020b
  [06]=70f4c1b19d9a9fc2
  [07]=a17baaebbc927a24
  [08]=eae0ad469b84142d
  [09]=4a1dfc7161dc7da2
  [10]=31da25eef77dbd8a
  [11]=7d120e6b363c357d
  [12]=d01799b6a9128315
)
score=0
for n in 01 02 03 04 05 06 07 08 09 10 11 12; do
  f="$LAB_DIR/broken/$n.graphql"
  [[ -s "$f" ]] || { fail "$n: broken/$n.graphql is missing"; continue; }
  v='{}'; [[ -f "$LAB_HOME/starter/broken/$n.json" ]] && v=$(cat "$LAB_HOME/starter/broken/$n.json")
  out=$(jq -Rs --argjson v "$v" '{query: ., variables: $v}' "$f" | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
  if jq -e '.errors' >/dev/null <<<"$out"; then fail "$n: still fails: $(jq -r '.errors[0].message' <<<"$out" | cut -c1-110)"; continue; fi
  h=$(jq -cS .data <<<"$out" | sha256sum | cut -c1-16)
  if [[ "$h" == "${KEY[$n]}" ]]; then score=$((score+1)); pass "$n"
  else fail "$n: valid now, but it does not return what the Intent line asks for"; fi
done
note "score: $score/12"
finish

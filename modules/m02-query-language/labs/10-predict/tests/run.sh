#!/usr/bin/env bash
source /course/tools/testlib.sh
# Each prediction is compared with what the reference server answers right now: data exactly
# (key order ignored), errors only by presence. Nothing here prints the right answer.
score=0
for c in "$LAB_HOME"/tests/cards/*.graphql; do
  n=$(basename "$c" .graphql); p="$LAB_DIR/predictions/$n.json"
  if ! jq -e . "$p" >/dev/null 2>&1; then fail "card $n: predictions/$n.json is not valid JSON"; continue; fi
  actual=$(jq -Rs '{query: .}' "$c" | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
  shape='{data: (.data // null), errors: (has("errors"))}'
  if [[ "$(jq -cS "$shape" "$p")" == "$(jq -cS "$shape" <<<"$actual")" ]]; then score=$((score+1)); pass "card $n"
  else
    hint=""
    [[ "$(jq -c 'has("errors")' "$p")" != "$(jq -c 'has("errors")' <<<"$actual")" ]] && hint="errors or no errors? think again"
    fail "card $n" "$hint"
  fi
done
note "score: $score/10. Group play: everyone writes predictions first, then run the test together."
finish

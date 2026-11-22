#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
KEY=$LAB_HOME/tests/stages.key.json
# "RFC 2 / Draft", "2", "Draft" all mean 2; the answers are hashed, so this never prints them.
norm() { local v; v=$(tr 'A-Z' 'a-z' <<<"$1" | sed -E 's/^ *rfc *//; s/^strawman.*/0/; s/^proposal.*/1/; s/^draft.*/2/; s/^accepted.*/3/; s/^([0-3x]) *(\/.*)?$/\1/; s/^ +| +$//g'); printf '%s' "$v"; }
right=0 total=0 wrong=()
for k in $(jq -r '.answers | keys[]' $KEY); do
  total=$((total + 1))
  v=$(jq -r --arg k "$k" '.[$k] // ""' stages.json 2>/dev/null)
  [[ $(norm "$v" | sha256sum | cut -c1-16) == $(jq -r --arg k "$k" '.answers[$k]' $KEY) ]] && right=$((right + 1)) || wrong+=("$k")
done
(( right == total )) && pass "stages.json: all $total stages as of $(jq -r .as_of $KEY)" || { fail "stages.json: $right of $total right (as of $(jq -r .as_of $KEY); look them up on rfcs.graphql.org)"; printf '        %s\n' "${wrong[@]}"; }
words=$(python3 -c 'import re,sys; print(re.sub(r"<!--.*?-->", "", open(sys.argv[1]).read(), flags=re.S))' EXPLAINER.md 2>/dev/null | grep -vE '^\s*#' | wc -w)
(( words >= 250 && words <= 350 )) && pass "EXPLAINER.md is $words words (250 to 350)" || fail "EXPLAINER.md is $words words; the wiki wants 250 to 350"
links=$(grep -oE 'https://(rfcs\.graphql\.org/rfcs/[A-Za-z0-9]+|github\.com/graphql/[a-z-]+/(pull|blob)/[^ )]+)' EXPLAINER.md 2>/dev/null | sort -u | wc -l)
(( links >= 2 )) && pass "EXPLAINER.md links the RFCs it explains ($links links)" || fail "EXPLAINER.md links at least two RFCs (rfcs.graphql.org/rfcs/... or the GitHub pull request)" "$links found"
notes_check READING.md 200 m18/05
finish

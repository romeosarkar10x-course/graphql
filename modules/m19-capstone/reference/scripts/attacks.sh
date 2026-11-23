#!/usr/bin/env bash
# Toolshed's self-run attack suite (docs/ATTACKS.md reports a run). Against the running stack, through the
# router, inside the course image:  scripts/attacks.sh [api]     (default http://localhost:4000/graphql)
# Prints one line per attack: id, what was tried, what came back, and BLOCKED or OPEN. Exit 1 if any is OPEN.
set -uo pipefail
cd "$(dirname "$0")/.."
API=${1:-http://localhost:4000/graphql}
source tests/ids.env
TOKEN=/course/modules/m10-security/contract/Token.java
tok() { java "$TOKEN" "$@"; }
open=0
post() { # body [token] -> body; HTTP status in /tmp/attack-status
  curl -s -m 10 -o /tmp/attack-body -w '%{http_code}' "$API" -H 'Content-Type: application/json' ${2:+-H "Authorization: Bearer $2"} --data-binary "$1" > /tmp/attack-status
  cat /tmp/attack-body
}
pd() { jq -nc --arg id "$1" --argjson v "${2:-{\}}" '{documentId: $id, variables: $v}'; }
report() { # id, attack, blocked-test (jq on the body), what to show (jq)
  local body=$5 shown; shown=$(jq -c "$4" <<<"$body" 2>/dev/null | cut -c1-90)
  if jq -e "$3" <<<"$body" >/dev/null 2>&1; then printf '%s  %-58s BLOCKED  %s\n' "$1" "$2" "$shown"
  else printf '%s  %-58s OPEN     %s\n' "$1" "$2" "$(cut -c1-120 <<<"$body")"; open=$((open + 1)); fi
}
code='.errors[0].extensions.classification // .errors[0].extensions.code'

report A01 "a query that is not a persisted document" '.errors and (.data == null)' "$code" \
  "$(post '{"query":"{ tools(first: 50) { edges { node { id barcode } } } }"}')"
report A02 "introspection, as a raw query" '.errors and (.data == null)' "$code" \
  "$(post '{"query":"{ __schema { types { name fields { name } } } }"}')"
report A03 "a made-up document id" '.errors and (.data == null)' "$code" "$(post '{"documentId":"0000"}')"
report A04 "an expired token" '.data.me == null and .errors' "$code" "$(post "$(pd "$MyLoans")" "$(tok ada --aud toolshed-api --expired)")"
report A05 "a token signed with another key, claiming the course kid" '.data.me == null and .errors' "$code" "$(post "$(pd "$MyLoans")" "$(tok ada --aud toolshed-api --forged)")"
report A06 "alg none, no signature" '.data.me == null and .errors' "$code" "$(post "$(pd "$MyLoans")" "$(tok ada --aud toolshed-api --none)")"
report A07 "a valid token for another API (aud shelfie-api)" '.data.me == null and .errors' "$code" "$(post "$(pd "$MyLoans")" "$(tok ada)")"
report A08 "OverdueReport at 50 x 50 (cost)" '.errors and (.data == null)' "$code" \
  "$(post "$(pd "$OverdueReport" '{"first":50,"history":50}')" "$(tok root --aud toolshed-api)")"
report A09 "a page of 10,000 tools" '.errors and (.data == null)' "$code" "$(post "$(pd "$ToolPage" '{"first":10000}')")"
report A10 "return someone else's loan (olu returns ada's l2)" '.data.returnTool.__typename == "LoanNotFound"' '.data.returnTool.__typename' \
  "$(post "$(pd "$ReturnTool" '{"loanId":"l2"}')" "$(tok olu --aud toolshed-api)")"
report A11 "a member reads who borrowed a tool (DeskLookup)" '.data.tool.currentLoan.member == null and (.errors[0].extensions.classification == "FORBIDDEN")' "$code" \
  "$(post "$(pd "$DeskLookup" '{"barcode":"TS-0005"}')" "$(tok olu --aud toolshed-api)")"
report A12 "a member reads the librarians' overdue report" '.data == null and (.errors[0].extensions.classification == "FORBIDDEN")' "$code" \
  "$(post "$(pd "$OverdueReport")" "$(tok ada --aud toolshed-api)")"
report A13 "SQL wildcards and quotes in search text" '(.errors | not) and (.data.search | length == 0)' '.data.search | length' \
  "$(post "$(pd "$Search" "{\"text\":\"%' OR 1=1 --\"}")")"
report A14 "a hand-made cursor with SQL in it" '.errors[0].extensions.classification == "BAD_REQUEST"' "$code" \
  "$(post "$(pd "$ToolPage" "{\"after\":\"$(printf "NAME_DESC\x1f' OR 1=1 --\x1ft01" | base64 -w0 | tr '+/' '-_' | tr -d =)\"}")")"
first=$(post "$(pd "$BorrowTool" '{"input":{"toolId":"t25","idempotencyKey":"shared-key"}}')" "$(tok ada --aud toolshed-api)")
report A15 "reuse another member's idempotency key to read their result" '.data.borrowTool.__typename == "ToolUnavailable"' '.data.borrowTool.__typename' \
  "$(post "$(pd "$BorrowTool" '{"input":{"toolId":"t25","idempotencyKey":"shared-key"}}')" "$(tok olu --aud toolshed-api)")"
report A16 "a batch of 50 operations in one request" '(type == "object" and .errors) or (type == "array" and length < 50)' 'if type == "array" then length else .errors[0].message end' \
  "$(post "[$(for i in $(seq 50); do printf '%s,' "$(pd "$ToolPage" '{"first":50}')"; done | sed 's/,$//')]")"
# The router merges identical in-flight subgraph requests (traffic_shaping dedupe, on by default): two members'
# concurrent "my loans" must not be merged into one answer.
ta=$(tok ada --aud toolshed-api); to=$(tok olu --aud toolshed-api); mixed=0
for round in 1 2 3; do
  for i in $(seq 20); do
    if (( i % 2 )); then t=$ta want="Ada Okafor"; else t=$to want="Olu Bankole"; fi
    ( b=$(curl -s -m 10 "$API" -H 'Content-Type: application/json' -H "Authorization: Bearer $t" --data-binary "$(pd "$MyLoans")")
      [[ $(jq -r '.data.me.displayName' <<<"$b") == "$want" ]] || echo x ) &
  done > /tmp/attack-dedupe; wait
  mixed=$((mixed + $(wc -l < /tmp/attack-dedupe)))
done
if (( mixed == 0 )); then printf 'A17  %-58s BLOCKED  %s\n' "60 concurrent MyLoans, Ada and Olu interleaved" "each got their own"
else printf 'A17  %-58s OPEN     %s\n' "60 concurrent MyLoans, Ada and Olu interleaved" "$mixed answers for the wrong member"; open=$((open + 1)); fi
me=$(hostname -i 2>/dev/null | awk '{print $1}')
direct=$(curl -s -m 3 -o /dev/null -w '%{http_code}' "http://$me:4002/graphql" -H 'Content-Type: application/json' --data '{"query":"{ overdueLoans { edges { node { id } } } }"}')
if [[ $direct == 000 ]]; then printf 'A18  %-58s BLOCKED  %s\n' "lending directly, past the router (http://$me:4002)" "connection refused"
else printf 'A18  %-58s OPEN     HTTP %s\n' "lending directly, past the router (http://$me:4002)" "$direct"; open=$((open + 1)); fi
echo "open: $open"
(( open == 0 ))

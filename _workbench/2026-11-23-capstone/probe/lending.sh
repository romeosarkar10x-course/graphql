source /course/_workbench/2026-11-23-capstone/probe/lib.sh
start_jar lending 4002 || exit 1
L=localhost:4002/graphql
tok() { java /course/modules/m10-security/contract/Token.java "$@" --aud toolshed-api; }
ADA="Authorization: Bearer $(tok ada)"; ROOT="Authorization: Bearer $(tok root)"; OLU="Authorization: Bearer $(tok olu)"
code() { jq -n '{query:"{ me { handle } }"}' | curl -s -o /dev/null -w "%{http_code}\n" $L -H "content-type: application/json" -H "Authorization: Bearer $1" --data-binary @-; }
echo "--- anon me, anon borrow"; q $L "{ me { handle } }"
q $L 'mutation { borrowTool(input: {toolId: "t03", idempotencyKey: "k1"}) { __typename } }'
echo "--- expired, wrong aud, forged, none"; code "$(tok ada --expired)"; code "$(java /course/modules/m10-security/contract/Token.java ada)"; code "$(tok ada --forged)"; code "$(tok ada --none)"
echo "--- ada me"; q $L '{ me { handle email loans(first: 5) { edges { node { id status tool { id } member { handle } } } } } }' '{}' "$ADA"
echo "--- borrows"
B='mutation($i: BorrowInput!) { borrowTool(input: $i) { __typename ... on Borrowed { loan { id status dueAt tool { id available } } } ... on Problem { message } ... on ToolUnavailable { expectedBack } ... on InvalidInput { field } } }'
q $L "$B" '{"i":{"toolId":"t03","days":3,"idempotencyKey":"k1"}}' "$ADA"
q $L "$B" '{"i":{"toolId":"t03","days":3,"idempotencyKey":"k1"}}' "$ADA"
q $L "$B" '{"i":{"toolId":"t05","idempotencyKey":"k2"}}' "$OLU"
q $L "$B" '{"i":{"toolId":"t99","idempotencyKey":"k3"}}' "$OLU"
q $L "$B" '{"i":{"toolId":"t04","days":30,"idempotencyKey":"k4"}}' "$OLU"
q $L "$B" '{"i":{"toolId":"t06","idempotencyKey":"k5"}}' "$ADA"
q $L "$B" '{"i":{"toolId":"t07","idempotencyKey":"k6"}}' "$ADA"
q $L "$B" '{"i":{"toolId":"t08","idempotencyKey":""}}' "$ADA"
echo "--- visibility"
q $L '{ loan(id: "l2") { id member { handle } } }' '{}' "$OLU"
q $L '{ _entities(representations: [{__typename: "Tool", id: "t05"}, {__typename: "Tool", id: "t09"}]) { ... on Tool { id available currentLoan { id member { handle email } } } } }' '{}' "$OLU"
q $L '{ _entities(representations: [{__typename: "Tool", id: "t05"}]) { ... on Tool { available currentLoan { id } } } }'
q $L '{ overdueLoans { edges { node { id } } } }' '{}' "$ADA"
q $L '{ overdueLoans(first: 1) { edges { node { id status member { handle } } } pageInfo { hasNextPage endCursor } } }' '{}' "$ROOT"
echo "--- return"
R='mutation { returnTool(input: {loanId: "l2"}) { __typename ... on Returned { loan { status } } ... on Problem { message } } }'
q $L "$R" '{}' "$OLU"; q $L "$R" '{}' "$ADA"; q $L "$R" '{}' "$ADA"
echo "--- N+1: entities 2 vs 20 with currentLoan member"
reps() { jq -nc --argjson n $1 '[range(1; $n+1) | {__typename: "Tool", id: ("t" + (if . < 10 then "0" else "" end) + (.|tostring))}]'; }
for n in 2 20; do c1=$(stmts 4002); q $L 'query($r: [_Any!]!) { _entities(representations: $r) { ... on Tool { available currentLoan { member { handle } status } } } }' "{\"r\": $(reps $n)}" "$ROOT" >/dev/null; c2=$(stmts 4002); echo "n=$n statements $c1 -> $c2"; done
grep -E "^Caused by" /tmp/lending.log | sort -u | cut -c1-300 | head -5

#!/usr/bin/env bash
cd "$(dirname "$0")"; export PATH="$PATH:/course/.cache/node/bin"
npx postgraphile -C graphile.hardened.mjs > /tmp/pgl.log 2>&1 & PID=$!; trap 'kill $PID' EXIT
for _ in $(seq 60); do (exec 3<>/dev/tcp/localhost/5678) 2>/dev/null && break; sleep 0.5; done
q() { curl -s localhost:5678/graphql -H 'Content-Type: application/json' -d "$(jq -n --arg q "$1" --argjson v "${2:-{\}}" '{query:$q, variables:$v}')"; echo; }
P='query($after: Cursor) { allClubs(first: 2, after: $after) { nodes { rowId } pageInfo { endCursor } } }'
c=$(q "$P" | tee /dev/stderr | jq -r .data.allClubs.pageInfo.endCursor)
q "$P" "{\"after\":\"$c\"}"
P2='query($after: Cursor) { allClubs(first: 2, after: $after, orderBy: ROW_ID_ASC) { nodes { rowId } pageInfo { endCursor } } }'
c=$(q "$P2" | tee /dev/stderr | jq -r .data.allClubs.pageInfo.endCursor); q "$P2" "{\"after\":\"$c\"}"
q '{ __type(name: "ReviewOrderBy") { enumValues { name } } __t: __type(name: "ReviewCondition") { inputFields { name } } }' | jq -c '[.data.__type.enumValues[].name], [.data.__t.inputFields[].name]'
q 'mutation { x }'; grep -i "error\|hash" /tmp/pgl.log | tail -3

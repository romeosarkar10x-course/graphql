#!/usr/bin/env bash
cd "$(dirname "$0")"; export PATH="$PATH:/course/.cache/node/bin"
npx postgraphile -C ${CFG:-graphile.config.mjs} > /tmp/pgl.log 2>&1 & PID=$!; trap 'kill $PID' EXIT
for _ in $(seq 60); do (exec 3<>/dev/tcp/localhost/5678) 2>/dev/null && break; sleep 0.5; done
q() { curl -s localhost:5678/graphql -H 'Content-Type: application/json' -d "$(jq -n --arg q "$1" '{query:$q}')"; echo; }
head -20 /tmp/pgl.log
echo "== query fields"; q '{ __schema { queryType { fields { name } } mutationType { fields { name } } } }' | jq -c '[.data.__schema.queryType.fields[].name], [.data.__schema.mutationType.fields[]?.name]'
echo "== club 1"; q '{ clubById(id: "1") { name readerByOwnerId { handle } bookByCurrentBookId { title } membershipsByClubId(first: 3, orderBy: JOINED_AT_ASC) { totalCount nodes { readerByReaderId { handle } } } } }'
echo "== books top"; q '{ allBooks(first: 2) { totalCount pageInfo { hasNextPage endCursor } nodes { id title reviewsByBookId { totalCount } } } }'
echo "== sdl"; curl -s localhost:5678/graphql -H 'Content-Type: application/json' -d '{"query":"{ __type(name: \"Club\") { fields { name type { name kind ofType { name } } } } }"}' | jq -c '.data.__type.fields | map(.name)'

#!/usr/bin/env bash
cd "$(dirname "$0")"; export PATH="$PATH:/course/.cache/node/bin"
npx postgraphile -C graphile.hardened.mjs > /tmp/pgl.log 2>&1 & PID=$!; trap 'kill $PID' EXIT
for _ in $(seq 60); do (exec 3<>/dev/tcp/localhost/5678) 2>/dev/null && break; sleep 0.5; done
curl -s localhost:5678/graphql -H 'Content-Type: application/json' -d '{"query":"{ __type(name: \"Club\") { fields { name type { kind name ofType { kind name } } } } q: __type(name: \"Query\") { fields { name args { name } } } }"}' | jq -c '[.data.__type.fields[] | "\(.name): \(.type.name // (.type.ofType.name + "!"))"], [.data.q.fields[] | select(.name == "allReviews") | .args[].name]'
curl -s localhost:5678/graphql -H 'Content-Type: application/json' -d '{"query":"{ allReviews(first: 1000) { nodes { rowId } } }"}' | jq -c '{n: (.data.allReviews.nodes|length), e: .errors}'
for p in / /graphiql /ruru; do curl -s -o /dev/null -w "$p: %{http_code}\n" -H "Accept: text/html" localhost:5678$p; done

cd /course/_workbench/2026-11-21-m08-probe/scroll
java -jar target/probe-scroll.jar > app.log 2>&1 & P=$!
for i in $(seq 60); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
q() { jq -n --arg q "$1" '{query:$q}' | curl -s -H 'Content-Type: application/json' -d @- localhost:4000/graphql; echo; }
q '{ books(first: 3) { edges { cursor node { id published } } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } } }'
C=$(q '{ books(first: 3) { pageInfo { endCursor } } }' | jq -r .data.books.pageInfo.endCursor); echo "cursor $C = $(echo $C | base64 -d)"
q "{ books(first: 2, after: \"$C\") { edges { node { id published } } pageInfo { hasNextPage hasPreviousPage } } }"
q "{ books(last: 2, before: \"$C\") { edges { node { id published } } pageInfo { hasNextPage hasPreviousPage } } }"
q '{ __type(name: "PageInfo") { fields { name type { kind name ofType { name } } } } }'
q '{ __type(name: "BookConnection") { fields { name type { kind ofType { kind ofType { kind ofType { name } } } } } } }'
grep -E "^Hibernate" app.log | tail -4
kill $P; wait $P 2>/dev/null

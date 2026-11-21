cd /course/modules/m08-pagination-errors/labs/03-spring-connections/${1:-solution}
java -jar target/shelfie-connections.jar > /tmp/conn.log 2>&1 & P=$!
for i in $(seq 80); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
q() { jq -n --arg q "$1" '{query:$q}' | curl -s -H 'Content-Type: application/json' -d @- localhost:4000/graphql; echo; }
q '{ books { edges { node { id } } pageInfo { hasNextPage hasPreviousPage } } }' | jq -c '[.data.books.edges[].node.id|tonumber], .data.books.pageInfo'
q '{ books(first: 3) { edges { cursor node { id published } } pageInfo { endCursor } } }'
E=$(q '{ books(first: 3) { pageInfo { endCursor } } }' | jq -r .data.books.pageInfo.endCursor)
q "{ books(first: 4, after: \"$E\") { edges { node { id published } } pageInfo { hasNextPage hasPreviousPage startCursor } } }"
q "{ books(last: 2, before: \"$E\") { edges { node { id } } pageInfo { hasNextPage hasPreviousPage } } }"
q '{ books(last: 3) { edges { node { id published } } pageInfo { hasNextPage hasPreviousPage } } }'
q '{ books(first: 101) { pageInfo { hasNextPage } } }'
q '{ books(first: -1) { pageInfo { hasNextPage } } }'
q '{ books(after: "bm9wZQ") { pageInfo { hasNextPage } } }'
q '{ books(after: "%%%") { pageInfo { hasNextPage } } }'
q '{ books(first: 0) { edges { cursor } pageInfo { hasNextPage } } }'
q "{ books(first: 2, before: \"$E\") { edges { node { id } } } }"
q "{ books(first: 2, last: 2) { edges { node { id } } } }"
grep -c Hibernate /tmp/conn.log; grep Hibernate /tmp/conn.log | sort | uniq -c
kill $P; wait $P 2>/dev/null

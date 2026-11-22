#!/usr/bin/env bash
cd "$(dirname "$0")"; export PATH="$PATH:/course/.cache/node/bin"
npx postgraphile -C graphile.hardened.mjs > /tmp/pgl.log 2>&1 & PID=$!; trap 'kill $PID' EXIT
for _ in $(seq 60); do (exec 3<>/dev/tcp/localhost/5678) 2>/dev/null && break; sleep 0.5; done
q() { curl -s localhost:5678/graphql -H 'Content-Type: application/json' -d "$(jq -n --arg q "$1" '{query:$q}')"; echo; }
q '{ clubByRowId(rowId: "1") { name readerByOwnerId { handle name } bookByCurrentBookId { title author } membershipsByClubId(first: 3, orderBy: JOINED_AT_ASC) { totalCount nodes { readerByReaderId { handle } } } } }'
q '{ allBooks { totalCount } allReviews(condition: { bookId: "1" }) { totalCount } readerByHandle(handle: "x") { name } }'
q '{ allClubs(first: 2, orderBy: ROW_ID_ASC) { nodes { rowId name } pageInfo { endCursor hasNextPage } } }'
c=$(q '{ allClubs(first: 2, orderBy: ROW_ID_ASC) { pageInfo { endCursor } } }' | jq -r .data.allClubs.pageInfo.endCursor)
q "{ allClubs(first: 2, after: \"$c\", orderBy: ROW_ID_ASC) { nodes { rowId name } } }"
q '{ clubByRowId(rowId: "10") { name bookByCurrentBookId { title } } }'
q '{ readerByRowId(rowId: "1") { handle name membershipsByReaderId { totalCount } reviewsByAuthorId(first: 2, orderBy: CREATED_AT_DESC) { totalCount nodes { rating bookByBookId { title } } } } }'
q '{ __schema { mutationType { name } } __type(name: "Query") { fields { name } } }' | jq -c '[.data.__schema.mutationType, ([.data.__type.fields[].name] | index("pick"))]'
q '{ allBooks(first: 1) { nodes { id } } }' | jq -r '.data.allBooks.nodes[0].id' | xargs -I{} sh -c "curl -s localhost:5678/graphql -H 'Content-Type: application/json' -d '{\"query\":\"{ node(id: \\\"{}\\\") { __typename ... on Book { title } } }\"}'"; echo
q 'mutation { deleteReviewByRowId(input: { rowId: "1" }) { deletedReviewId } }'
q '{ allReviews(first: 3, orderBy: RATING_DESC) { nodes { rating } } }'

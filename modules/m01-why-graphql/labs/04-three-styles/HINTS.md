# Hints, lab 1.4

1. `curl -fsS URL | jq -r .name` prints a raw string. `grpcurl -plaintext -d '{"id":"c1"}' HOST:PORT package.Service/Method` calls one RPC; `grpcurl -plaintext HOST:PORT describe shelfie.v1.GetClubPageRequest` shows a message's fields.
2. For GraphQL, let `jq -n --arg id "$CLUB" '{query: "...", variables: {id: $id}}'` build the body so you never hand-escape JSON, and pipe it to `curl --data-binary @-`.
3. grpcurl prints the `.proto` field names as written (`member_count`), not camelCase. For the matrix, ask of each scenario: who are the clients, how many are there, and who controls them?

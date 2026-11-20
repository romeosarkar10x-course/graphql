# Lab 0.4 notes

## 1. The three status codes
For each wrong request in status-codes.txt: what does the code mean, and is the server allowed to answer
that way by the GraphQL-over-HTTP spec? (https://graphql.github.io/graphql-over-http/draft)

## 2. The 200 that was an error
Send `{ nope }` with `Accept: application/json` and again with `Accept: application/graphql-response+json`.
Record both status codes. Why might the spec want the second one to differ?

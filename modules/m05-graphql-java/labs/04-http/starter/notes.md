# Lab 5.4 notes

## Audit, before and after

| | Checks passing |
|---|---|
| Starter server | /25 |
| Your server | /25 |

## Questions

1. Pick three audit failures of the starter and explain what a real client or proxy would get wrong
   because of each.
2. The September 2026 edition recommends `294` for "data plus errors". Why does the server only send it
   with `application/graphql-response+json`, and why is a legacy `application/json` client sent `200`?
3. Your server answers a JSON parse failure with plain text, but a GraphQL syntax error with
   `application/graphql-response+json`. What is the rule that separates the two?
4. The reference server (Spring for GraphQL 2.1) answers GET with 405 (Module 0.4). Is that compliant?
   What would you lose and gain by turning GET off in your own server?

# Rubric, lab 11.6 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | Time to first byte and total from real runs, with and without `@defer` and for a JSON client, and what the user sees earlier. Invented timings score 0. |
| 3 | graphql-java 26.1's payloads next to RFC 1110's (`pending` with `id`/`path`/`label`, `incremental` by `id`, `completed`, `hasNext`), and what the difference means for clients. |
| 2 | What Spring for GraphQL 2.1 did with the flag on (initial payload only, deferred data lost; 415 for multipart) and why the fix enables the flag per request. |
| 2 | When to defer and when not to, and the state of `@stream` in graphql-java 26.1. |

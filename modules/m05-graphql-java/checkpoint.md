# Checkpoint 5 quiz

1. Name graphql-java's three phases and the kind of error each one can produce.
2. A response has `"data": null` and one error. Was it a request error? What happened?
3. What does `PropertyDataFetcher` read, and in which order does it look?
4. Ten async sibling fields run on a fixed pool of 4 threads, 200 ms each. About how long, and why?
5. Under GraphQL over HTTP (September 2026), what status does a validation error get? And data plus
   errors, for a client that only accepts `application/json`?
6. `beginFieldFetch` says a field took 60 ms; `beginFieldExecution` says 800 ms. Which one tells you
   what to optimise first?
7. Why is a `ThreadLocal` the wrong place for the viewer, and what is the right place?
8. Your document cache is keyed on query text. What two properties must it have to be safe?

<details><summary>Answers</summary>

1. Parse (syntax error, a request error), validate (validation error, a request error), execute (field
   errors with partial data).
2. No: a request error would remove `data` entirely. Execution ran, and a field error propagated through
   non-null fields up to the root.
3. A record component, a getter, a public field or a map key with the field's name (getter-style access
   first, then fields, maps by key).
4. About 600 ms: three waves (4 + 4 + 2) of 200 ms; the pool size caps the overlap.
5. 422. 200 with `Content-Type: application/json` (294 only with the GraphQL media type).
6. `beginFieldFetch`: execution time includes the whole subtree, so every ancestor of a slow field looks
   slow too.
7. Async fetchers run on other threads that never saw the value; put it in `GraphQLContext`, which travels
   with the execution.
8. Bounded (an attacker controls the keys) and per schema (the cached entry skips validation).
</details>

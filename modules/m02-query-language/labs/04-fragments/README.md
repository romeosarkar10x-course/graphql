# Lab 2.4 — Fragments

**Lecture:** [2.4 Fragments](../../lectures/2.4-fragments.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

`profile-page.graphql` is 70 lines of repetition: the same user chip, book tile and club row, typed out
again and again. Refactor it into named fragments without changing a single byte of the response.

## Do

1. `bin/course start m02/04`, then work in `workspace/m02-query-language/04-fragments/`.
2. Save the original output first, in GraphiQL or with curl (variables `{"id": "u1", "q": "the"}`):

   ```bash
   jq -Rs '{query: ., variables: {id: "u1", q: "the"}}' profile-page.graphql \
     | curl -s -H 'Content-Type: application/json' -d @- localhost:8080/graphql | jq -c . > before.json
   ```

3. Refactor into at least 4 named fragments, at least one of them spread in two places, and keep the
   inline fragments' type conditions working (`search` returns a union). Compare with `before.json`
   after every step.
4. Write `fragment-cycle.graphql`: a fragment that ends up spreading itself.
5. Answer the questions in `notes.md`.

## Done when

`bin/course test m02/04` is green (the test compares bytes for two sets of variables) and
`bin/course grade m02/04` gives 7/10 or more.

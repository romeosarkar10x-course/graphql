# Checkpoint 15

`bin/course checkpoint 15` first runs your mini-project 15.8 on your machine, `docker compose up --wait` in
`workspace/m15-federation/08-project-team`: four subgraphs and one router, up in one command, answering the club
page on `localhost:4000` (then `docker compose down`). Then it checks labs 15.3 to 15.7 and 15.8, and tags
`checkpoint-15`. Port 4000 must be free. Answer these first, without looking.

1. What makes a type an entity, and what does its key let other teams do?
2. Catalog's `Book` loses its `@key`. What does composition say, and why that?
3. Reviews answers `_entities` for 15 reviews with the single `@EntityMapping` form. How many requests does the
   router send, and how many queries does reviews run?
4. `Club.books.weeksToRead` needs catalog's `pages`. How many steps, in which order?
5. Hydration in Nadel against `@key` in federation: who owns `Review.book` in each?
6. A subgraph hangs. What keeps the page answering, and what does the client get?
7. Move a field between subgraphs with zero errors: the steps.
8. Why is `docker compose up --wait` not enough on its own to prove the stack works?

<details><summary>Answers</summary>

1. `@key(fields: ...)`: several subgraphs may then declare the type and add fields; the router fetches it from its
   owner by key with `_entities`.
2. `INVALID_FIELD_SHARING` on `Book.id` (resolved by several subgraphs, shareable in none): without a key the
   types are no longer one entity, just clashing definitions. With an entity interface present, the first error
   is `INTERFACE_KEY_NOT_ON_IMPLEMENTATION`.
3. One request (all representations at a path in one call); 15 queries, unless the List or DataLoader form batches
   them into one.
4. Three: clubs (the club), catalog (`pages`, by key, in parallel with any accounts fetch), clubs again
   (`weeksToRead` with `pages`, `@requires`).
5. Nadel: the gateway's schema (`@hydrated`). Federation: reviews, which declares `Review.book: Book` and relies
   on Book's key.
6. A subgraph timeout in the router and a nullable field: the field is null with an error, the rest of the page is
   there.
7. The new owner claims it with `@override(from:)`; the old owner removes it; the new owner drops `@override`. One
   team per deploy, each state composes.
8. Healthy containers only say each process answers its health check; the club page through the router proves the
   composition, routing and headers. The checkpoint asks it.

</details>

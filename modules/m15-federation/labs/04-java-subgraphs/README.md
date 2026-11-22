# Lab 15.4: Java subgraphs

Lecture: [15.4 Java subgraphs](../../lectures/15.4-java-subgraphs.md). Grading: auto + rubric. Needs the
federation tools (`bin/course fed`).

Make catalog and reviews real subgraphs on Spring for GraphQL. One jar, two profiles:
`--spring.profiles.active=catalog` (port 4002) and `reviews` (4003). Their schemas are given
(`graphql/catalog`, `graphql/reviews`, the contract's SDL); so is `Store`, the "database", which counts every
query it answers (`GET /_stats`). The starter does not even start: the schemas use federation directives that
plain Spring for GraphQL does not know.

1. Plug in federation: a `FederationSchemaFactory` bean (federation-jvm 7.0.0) used by the `GraphQlSource`.
2. Resolve entities with `@EntityMapping`, in all three forms Spring offers, where each fits:
   - catalog's `Book`: **List** form, one store query for all representations, whichever key each uses (`id` or
     `isbn`);
   - reviews' `Review`: **DataLoader** form;
   - reviews' `Book` and `User`, which are only keys there: **single** form.
3. Batch the fields reviews adds to books: `reviews`, `reviewCount` and `averageRating` together cost **one**
   store query per level, however many books and however many of the three fields a query asks for.
4. Unknown ids resolve to `null`, and never to an `INTERNAL_ERROR`.

The test starts your two subgraphs and the contract's accounts and clubs, composes the supergraph from what
yours serve at `{ _service { sdl } }`, runs Apollo Router, and checks answers and `/_stats` both directly and
through the router.

```bash
bin/course start m15/04
cd workspace/m15-federation/04-java-subgraphs
bin/course shell -c 'mvn -q package && (java -jar target/shelfie-java-subgraphs.jar --spring.profiles.active=catalog --server.port=4002 &) && (java -jar target/shelfie-java-subgraphs.jar --spring.profiles.active=reviews --server.port=4003 &) && SKIP=catalog,reviews bash /course/modules/m15-federation/contract/stack.sh'
bin/course test m15/04
```

The experiment: count the store queries an `_entities` request costs with the single form against the batched
ones (switch catalog's form, rerun, read `/_stats`). In `notes.md`: the numbers, when each form fits, and the
DGS equivalent (`@DgsEntityFetcher`).

## Done when

`bin/course test m15/04` passes and `notes.md` passes the rubric (`bin/course grade m15/04`).

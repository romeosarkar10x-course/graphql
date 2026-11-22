# Same screen, three clients

Reference answer (grader only), assembled from the reference solutions of labs 12.2, 12.6 and 12.7 on
2026-11-22. Line counts from `wc -l`; behaviour from the runs saved in `_workbench/2026-11-22-m12-probe/`.

## The feature

The card: `club(id: "c1") { name memberCount currentBook { id title averageRating } }`. Add review:
`createReview(input: {bookId: "b1", rating: 1, body: ...})` as user u11, selecting `review { id book { id
averageRating } }`. Expected: 4.2 before, 3.67 after.

## Side by side

| | Spring client | Apollo Kotlin | Web (Apollo Client 4 / Relay 20) |
|---|---|---|---|
| Lines you wrote | 110 Java (controller, clients, records) + 32 in two document files | 73 Kotlin + 35 GraphQL (operations, type policies) + 27 Gradle | Apollo 40 JSX; Relay 86 JS/JSX over four files; plus the harness |
| Generated or done for you | decoding into records (`toEntity`), document loading, interceptors | Kotlin models for each operation, the normalised cache in SQLite, fetch policies | Apollo: normalised cache, re-render on change. Relay: generated artifacts, masking, store subscriptions |
| Requests: card, review, card again | 3: no cache, so the card is fetched again (or the service keeps the mutation's average itself) | 2: the second card is `CacheOnly` | 2 (measured in the harness) |
| New average via | the mutation payload (the service has to merge it by hand) or a refetch | the normalised record `Book:b1`, once `@typePolicy(keyFields: "id")` was declared | the normalised record; Apollo with masking and Relay re-rendered only `BookPanel` |
| What broke | `SHELFIE_URL` bound to `shelfie.url` by relaxed binding; `documentName` sends no `operationName` | without `keyFields` the cache kept 4.2 after sync | relay-compiler panic on `.graphqls`; Node ESM needs a resolve hook; react-relay is CommonJS |
| Build and tooling | Maven only | Gradle + Kotlin + Apollo plugin; 249 MB in `.cache/gradle` after the first build (Gradle, Kotlin, Apollo) | npm, relay-compiler, Babel; `npm ci` installs 194 packages |

## Picks

- **Web app:** Apollo Client 4 with data masking and colocated fragments: Relay's render behaviour without a
  compiler step. Relay if the app grows large and the team wants the compiler's guarantees.
- **Android app:** Apollo Kotlin: codegen against the schema, a persistent normalised cache, `CacheOnly` and
  `CacheFirst` for offline. Add an outbox; Apollo has none.
- **Backend calling Shelfie:** Spring's `HttpSyncGraphQlClient` with document files (or DGS codegen when many
  services share a big schema): no client cache, because a backend should not hold a stale copy of another
  service's data; cache at the HTTP layer or ask the owning service for events instead.
- What would change it: a backend that renders pages for users (then a normalised cache per request can save
  calls), or a small app where urql's simpler document cache (lecture 12.6) is enough.

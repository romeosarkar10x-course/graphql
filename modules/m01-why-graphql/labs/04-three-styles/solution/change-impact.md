# Lab 1.4 — Change impact (reference)

## REST
- Store: a `pagesRead` per member and book. Controller: either a new
  `GET /api/clubs/{id}/progress` returning `[{userId, pagesRead}]`, or a field on `/api/users/{id}`
  (wrong: progress is per club book, not per user).
- New endpoint: one more request (wave 2) for the page. Putting it on the club resource instead
  grows every club response for every client, including those that never show progress.
- Nothing breaks (additive JSON), but the "add it to the club" option makes every client over-fetch more.

## gRPC
- `shelfie.proto`: add `repeated MemberProgress progress = 11;` to `ClubPage` (new message), implement in
  `ShelfieGrpcService.getClubPage`. Resource-RPC clients would need a new `ListClubProgress` RPC too.
- Old clients keep working: unknown field numbers are skipped by Protobuf decoders. But every screen that
  needs progress must wait for the backend team to extend its bespoke RPC: endpoint sprawl again.

## GraphQL
- `schema.graphqls`: a new type and field, for example
  `type MemberProgress { member: User! pagesRead: Int! }` and `Club.readingProgress: [MemberProgress!]!`
  (or `User.progress(book:)`). One new `@SchemaMapping` resolver.
- Existing queries are unchanged and receive exactly the same bytes; only clients that select the new field
  pay for it. The club page adds it to its one query: still 1 request.

## Verdict
GraphQL was cheapest for the *client* teams: one additive field, no new round trip, no impact on others.
The cost moved to the server: the new resolver must be efficient when 30 members × N clubs ask at once
(N+1, Module 9) and must enforce who may see whose progress (Module 10).

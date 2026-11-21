# Lab 7.2 — One id for everything

**Lecture:** [7.2 Global object identification](../../lectures/7.2-global-ids.md) · **Grading:** auto · **Needs:** nothing

Shelfie's ids are `c1`, `u1`, `b1`, `r1`, `m1`: unique per table, not across the graph. A client cache
that keys objects by `id` can't tell `Club c1` from a `Book c1`, and a client holding only an id can't
fetch the object again. Global object identification fixes both: every cacheable object implements
`Node`, its `id` is opaque and unique across types, and `node(id:)` fetches any of them again.

## Do

1. `GlobalId.java`: `record GlobalId(String type, String key)`.
   - `encode()`: unpadded **base64url** of `"Type:key"` (`Club:c1` becomes `Q2x1YjpjMQ`). Base64 is
     not secrecy, only a signal: "don't parse this".
   - `decode(String)`: `Optional.empty()` for anything that isn't one of ours: bad base64, no `:`, a
     type that isn't PascalCase, an empty key. The key may contain `:` (`Review:r:15`).
2. `nodes.graphqls`: `interface Node { id: ID! }`; `Club`, `User`, `Book`, `Review` and `Meetup`
   implement it; `node(id: ID!): Node` and `nodes(ids: [ID!]!): [Node]!` on `Query`.
3. `Nodes.java`: a **registry**, one entry per type: Java class, loader by key, key getter. Use it in
   three places: every `id` fetcher (object to global id), `node`/`nodes` (global id to object, null when
   unknown) and `Node`'s type resolver (Java class to GraphQL type).
4. Make sure `Book:c1` (a club's key with a book's type) is null, not the club.

`Data.java` is the Module 5 seed with `review(id)` and `meetup(id)` added. Don't edit it.

## Experiment

Refetch 5 different object types through `node(id:)`. The test does it silently; to watch it, write a
small `Refetch.java` whose `main` runs `{ clubs { id owner { id } currentBook { id reviews { id } } meetups { id } } }`,
then `node(id:)` with each of the five ids, printing what comes back. Run it with
`../../../bin/course java Refetch.java`: Java 25 compiles the other `.java` files in the folder with it.
Then decode one id by hand (`echo Q2x1YjpjMQ | base64 -d`) and think about what you'd lose if clients
started building ids themselves.

## Done when

`bin/course test m07/02` is green.

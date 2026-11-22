# Lab 15.2 notes

## Who owns Review.book

Neither service. Reviews stores `bookId`; catalog answers `books(ids:)`; the link between them exists only in the
gateway's overall schema (`reviews.nadel`):

```graphql
book: Book @hydrated(service: "catalog", field: "books",
                     arguments: [{ name: "ids", value: "$source.bookId" }], identifiedBy: "id")
```

Nadel asked reviews for `latestReviews { id rating batch_hydration__book__bookId: bookId }` (adding the key under
an alias), then catalog once for `books(ids: ["b8", "b3", "b9"])` for five reviews: the batch removed the
duplicate `b9`. `bookId` never reaches the client. With the unbatched form (`field: "book"`, one id), the same
query made 4 calls to catalog for 4 reviews, including `b9` twice.

So the gateway team owns the relationship: its schema, its batching, its failure when catalog's `books` changes
shape. That is gateway-side linking. In federation (lab 15.3) reviews would declare `Review.book: Book` itself,
with `Book @key(fields: "id")`, and catalog would answer `_entities`: the subgraph owns the link and composition
checks it.

## The rename: who decides

`name: String! @renamed(from: "title")` in the gateway's schema renamed the field for every client at once:
`{ book { title } }` now fails validation ("Field 'title' in type 'Book' is undefined"). Catalog did not change
and was not asked: Nadel sends `rename__name__title: title` underneath. The gateway team decided an API change
for catalog's data, and catalog cannot see who uses `name`. Convenient for fixing a bad name without touching a
service; dangerous because the owner of the data is no longer the owner of its contract. In federation the
rename is catalog's PR, and composition, not a gateway team, checks it.

## Stitching against federation

| | stitching / hydration (Nadel) | federation |
|---|---|---|
| services | plain GraphQL, know nothing of each other | subgraphs with `@key`, `_entities`, `_service` |
| links | in the gateway's schema | in the subgraphs' schemas |
| who changes a link | the gateway team | the subgraph team |
| checks | Nadel validates overall against underlying at start-up | composition, before deploy |
| fits | many existing services you cannot change; a central API team (Atlassian) | teams that own their part of the graph (Netflix) |

Nadel is the JVM's own gateway (Kotlin, Apache-2.0): it runs inside a JVM process, so policy and auth code in
Java can run in the gateway itself.

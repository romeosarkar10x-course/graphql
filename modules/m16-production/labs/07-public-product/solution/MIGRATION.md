# Migrating from 2026-10 to 2027-01

Three changes need action. Everything else in 2027-01 is new and optional. You have until **2027-10-01**, when
2026-10 stops being supported; after that, requests for 2026-10 are answered by the oldest supported version,
and queries that use the removed fields fail validation.

Check your operations against the new schema before you switch: `bin/course gql validate your-query.graphql
--schema 2027-01.graphqls` fails on every field below.

## 1. `Book.isbn10` is gone: use `isbn13`

Before (2026-10):

```graphql
query BookScreen($id: ID!) { book(id: $id) { title isbn10 averageRating } }
```

After (2027-01):

```graphql
query BookScreen($id: ID!) { book(id: $id) { title isbn13 averageRating } }
```

If you show ISBN-10 to users, compute it from the ISBN-13 (drop the `978` prefix and the check digit, then add the
mod-11 check digit). Books whose ISBN-13 starts with `979` have no ISBN-10.

## 2. `Club.memberCount` is gone: use `memberTotal`

Before:

```graphql
query ClubList { clubs { id name memberCount } }
```

After:

```graphql
query ClubList { clubs { id name memberTotal } }
```

Same value, same type. If you cache clubs by field name, clear the cache when you switch.

## 3. `addReview` needs `visibility`

Before:

```graphql
mutation Rate($book: ID!) { addReview(bookId: $book, rating: 4, body: "Loved it") { id seq rating } }
```

After: pass `PUBLIC` to keep today's behaviour, or let the author choose `CLUBS`.

```graphql
mutation Rate($book: ID!, $visibility: ReviewVisibility!) {
  addReview(bookId: $book, rating: 4, body: "Loved it", visibility: $visibility) { id seq rating }
}
```

## How to switch safely

1. Change the version in your URL from `2026-10` to `2027-01` in a build behind a flag.
2. Run your operations against the new version (or validate them against the published schema).
3. Roll out; watch for `GRAPHQL_VALIDATION_FAILED` errors in your error reporting.

# Normalised caching: notes

Reference notes (grader only), from the reference solution's run of `LiveDemo.java` on 2026-11-22.

## One write, every query

```
before: book page 4.2, club page 4.2
after:  book page 3.67, club page 3.67 (Shelfie says 3.67)
store:  {
  "ROOT_QUERY" : { "book({\"id\":\"b1\"})" : { "__ref" : "Book:b1" }, "club({\"id\":\"c1\"})" : { "__ref" : "Club:c1" } },
  "Book:b1" : { "__typename" : "Book", "id" : "b1", "title" : "The Left Hand of Darkness", "averageRating" : 3.67 },
  "Club:c1" : { "__typename" : "Club", "id" : "c1", "name" : "The Midnight Readers", "currentBook" : { "__ref" : "Book:b1" } },
  "Review:r16" : { "__typename" : "Review", "id" : "r16", "rating" : 1, "book" : { "__ref" : "Book:b1" } }
}
```

Both pages reach `Book:b1`: the book page through `ROOT_QUERY.book({"id":"b1"})`, the club page through
`Club:c1.currentBook`. The mutation's response contained `book { __typename id averageRating }`, so writing it
merged `averageRating: 3.67` into that one record; neither page stores a copy. It works only because the
mutation selected `__typename` and `id` (and the field that changed): without the id the book in the
response would have been an embedded object and nothing would have changed.

## What the cache cannot know without a schema

- **Default arguments.** `members` and `members(first: 10)` are the same list on the server (default 10),
  but the cache stores them under `members` and `members({"first":10})`. With the schema, Apollo Kotlin's
  codegen and Relay's compiler know the defaults; Apollo Client relies on `keyArgs` field policies.
- **Two fields, one entity.** `book(id: "b2")` after `books(first: 2)` already fetched b2 is a miss: the
  cache cannot know that `book(id:)` returns `Book:<id>`. Apollo Client's `read` field policy returning
  `toReference` (scenario 17), Apollo Kotlin's `@fieldPolicy(forField: "book", keyArgs: "id")` fix it.
- Also: which types are entities (an object without `id` but with an `isbn`, `keyFields`), and which abstract
  types an object belongs to (`possibleTypes`, generated from the schema).

## What one write does not update

After `createReview`, `Book:b1` has the new average but `Book:b1.reviews({"last":5})` still lists the old
reviews (scenario 18). The response says a review exists and which book it belongs to; it does not say which
lists, with which arguments, it now belongs in (or at which position: `last: 5` means it pushes one out).
Options: refetch the affected queries (`refetchQueries`), write the list back yourself in an update function
(read, add the ref, write: scenario 19), return the whole updated list from the mutation, evict the list field
so the next read misses and refetches (scenario 21), or subscribe to changes.

# Lab 7.3 notes

## 1. Keys you removed

- `Club.ownerId` became `owner: User!`, `Club.currentBookId` became `currentBook: Book`,
  `Review.authorId`/`bookId` became `author: User!`/`book: Book!`. The client follows a field instead of
  issuing a second request and joining by hand.
- `Club.memberIds` and `memberRoles` (two parallel lists the client had to zip by position) became one
  `members` connection whose edges hold the user and the role together.
- `User.clubIds` became `clubs: [Club!]!`.

## 2. List or connection?

- `Club.members`: connection. A public club can reach thousands of members, the screen shows 5 and
  pages the rest, and the pair carries data (role, join date).
- `User.clubs`: list. A person belongs to a handful of clubs; nobody pages that. If it ever needs the role,
  it becomes `memberships` (a list of membership objects), still not a connection.
- `Book.authors`: list. Bounded by the cover, no edge data, never paged.
- `Book.reviews`: connection. Unbounded, newest first, and the book page shows 3 and "more".
Changing a list into a connection later is breaking (the field's type changes), so decide by the
largest realistic size, not today's seed.

## 3. Edge data

`role` is a fact about a pair: Asha is MODERATOR in the Midnight Readers and an ordinary MEMBER of
the Booker club. On `User` there is one `role` per person, so it is wrong for every club but one; on
`Club` it would need a map. On the edge it is exactly one value per pair. `joinedAt` is the same: a person
joins each club on a different day.

## 4. Experiment: the client update

Before: three operations (`ReviewCard`, then `ReviewAuthor` and `ReviewBook` with the ids from the
first response), so two round trips, 13 lines. After: one operation, one round trip, 9 lines.
Against the new schema, `bin/course design clients relations.graphqls` on the starter's
`review-card.graphql` reports `broken`: "Field 'authorId' in type 'Review' is undefined" and the same for
`bookId`. That is the breaking change made visible: removing a field breaks every client that selects
it, so in production `authorId` would stay, deprecated, until no client sends it (lecture 7.6).

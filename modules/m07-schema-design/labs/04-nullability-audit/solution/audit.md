# Lab 7.4 audit

## The checklist

Ask these of every field, in order. The first "yes" decides.

1. Can the value be **absent in correct data** (no cover, signed out, nothing planned)? Nullable.
2. Does the value come from **another service or query** than its parent, so it can fail on its own?
   Nullable for the error's sake; if it is never absent in correct data, add `@semanticNonNull`.
3. Neither: it comes with the parent's row. Non-null: a promise you can keep.

## The audit

| Field | 1. Absent in data? | 2. Fails alone? | Decision | Why |
|---|---|---|---|---|
| Query.club | yes: unknown id | - | `Club` | Not found is an answer, not an error |
| Club.id | no | no: the row | `ID!` | Same row as the club |
| Club.name | no | no: the row | `String!` | Same row; a club without a name is invalid data |
| Club.description | yes: optional | - | `String` | Owners may leave it empty |
| Club.memberCount | no | yes: membership service | `Int @semanticNonNull` | Always a number, but computed elsewhere |
| Club.viewerIsMember | yes: signed out | - | `Boolean` | Depends on the caller; also fails with membership |
| Club.owner | no | no: `ownerId` is on the row | `User!` | The object can be built from the id; its fields fail individually |
| Club.currentBook | yes: between books | - | `Book` | "Choosing the next book" is a real state |
| Club.nextMeetup | yes: none planned | - | `Meetup` | Also fails with the meetup service |
| Club.members | no | yes: membership service | `[User!] @semanticNonNull` | A club always has its owner as a member; the list comes from another service. Items stay `User!`: a member is an id |
| User.id | no | no | `ID!` | The key we looked it up by |
| User.name | no | yes: profile service | `String @semanticNonNull` | Everyone has a name; profiles can be down |
| User.avatarUrl | yes: no picture | - | `String` | Show initials |
| Book.id | no | no | `ID!` | Key |
| Book.title | no | no: catalogue row | `String!` | Same row as the book |
| Book.coverUrl | yes: publisher sent none | - | `String` | Show a placeholder |
| Book.authors | no | no: catalogue row | `[Author!]!` | Stored with the book |
| Book.averageRating | yes: no reviews | - | `Float` | Also fails with the ratings service |
| Author.id, Author.name | no | no | `!` | Catalogue row |
| Meetup.id, startsAt, venue | no | no | `!` | The meetup's own row; the meetup as a whole is what fails |

## Experiment: kill one dependency

`bin/course design outage club.graphqls club-page.graphql --fail <fields>`, 25 values on a healthy page:

| Outage (fields that throw) | Starter (all `!`) | Audited |
|---|---|---|
| ratings: `Book.averageRating` | 0 | 24 |
| profiles: `User.name`, `User.avatarUrl` | 0 | 19 |
| meetups: `Club.nextMeetup` | 0 | 22 |
| membership: `viewerIsMember`, `memberCount`, `members` | 0 | 17 |

With every field non-null, any failure propagates to `Query.club!` and `data` itself is null: a blank
page for a missing star rating. Audited, each outage removes exactly the values that failed. Membership
hurts most (8 values) because it backs three fields including a list; profiles next (6) because
`User.name` appears once per person on the page, so one service failure removes many values.

## The other fix: error behaviour

The starter schema with `--no-propagation` gives 24, 19, 22 and 17: the same counts as the audited
schema, without changing a `!`. Errors stay where they happen and the parent is not nulled. If clients
could always ask for this (`onError: NULL` in RFC #1163), `!` would go back to meaning "never null in
correct data", the semantic meaning, and a null in a `!` position would always come with an error in
`errors`. That is what `@semanticNonNull` writes down today for clients that still get the default
PROPAGATE behaviour (the working group decided on 2026-08-06 that PROPAGATE stays the default): codegen
for those clients can type `User.name` as non-null and throw on the error, instead of making every
caller handle a null that only an outage can produce.

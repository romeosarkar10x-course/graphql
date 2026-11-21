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
| Query.club | | | | |
| Club.id | | | | |
| Club.name | | | | |
| Club.description | | | | |
| Club.memberCount | | | | |
| Club.viewerIsMember | | | | |
| Club.owner | | | | |
| Club.currentBook | | | | |
| Club.nextMeetup | | | | |
| Club.members | | | | |
| User.id | | | | |
| User.name | | | | |
| User.avatarUrl | | | | |
| Book.id | | | | |
| Book.title | | | | |
| Book.coverUrl | | | | |
| Book.authors | | | | |
| Book.averageRating | | | | |
| Author.id, Author.name | | | | |
| Meetup.id, startsAt, venue | | | | |

## Experiment: kill one dependency

Your counts from `design outage` for each of the four outages, starter schema vs yours. Which outage hurts
most, and why that one?

## The other fix: error behaviour

Run the starter schema with `--no-propagation` (graphql-java's `@experimental_disableErrorPropagation`,
the shape of `onError: NULL` from RFC #1163). Compare the counts with your audited schema. If clients
could always ask for that, what would `!` mean, and why does `@semanticNonNull` still matter?

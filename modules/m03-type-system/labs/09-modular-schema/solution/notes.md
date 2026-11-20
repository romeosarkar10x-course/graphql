# Lab 3.9 notes (reference)

## How you split it

`00-base` (schema, scalars, Node, Timestamped, root types), `clubs` (Club, Visibility, ClubRules,
MeetingCadence, Meetup, joinClub), `books` (Book, Review, SpoilerLevel, BookLookup, createReview),
`users` (User, ShelfEntry, ReadingStatus), `feed` (FeedItem and members, `extend type Club { feed }`).
Hard one: `Review` touches books and users; it sits with books because a review cannot exist without a
book, and `User.reviews` only points at it. Extending `Club` from `feed` keeps the feed team's field in
the feed team's file.

## Deprecations

`Club.size` → `memberCount`; `Query.books(titleContains:)` → `search`; `CreateReviewInput.spoiler` →
`spoilerLevel`; `ReadingStatus.ON_HOLD` → `READING`. Safe to remove when field-usage metrics
(Module 16) show no operation used the coordinate for a full release cycle of the oldest supported
client, i.e. per coordinate, which is exactly why lint and metrics key on coordinates.

## Directives on directives

`InvalidSyntaxError ... offending token '@'` (graphql-java 26.1, probe 9d). Not wrong: RFC #1206 was
accepted in 2026 and is not in the September 2025 spec graphql-java implements. Spec first, then
implementations.

## Question

One canonical string per element (the spec says each element has exactly one coordinate), usable as a
key in configs, logs, metrics and docs; standard parsing; it covers enum values, input fields and
directive arguments with the same syntax, and tools agree on it.

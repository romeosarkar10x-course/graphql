# INTENT.md — what player A meant (open only after guesses.md is written)

| Field | Intent |
|---|---|
| `Club.size` | Members **other than the owner** (the owner is staff, not a member, on the club page) |
| `Club.active` | Has at least one upcoming meetup |
| `Club.nextMeetup` | The earliest **upcoming** meetup by `startsAt`; null when none |
| `User.latestReview` | The user's most recent review by `createdAt` |
| `User.since` | `createdAt` of the user's **first** review: "reviewing since"; null if they never reviewed |
| `Book.rating` | The **viewer's own** rating of the book (GraphQLContext key `viewer`); null without a viewer or a review |
| `Book.popular` | At least 3 reviews **and** an average of 4.0 or more |
| `Query.recent(n)` | The `n` newest reviews across all books, newest first |

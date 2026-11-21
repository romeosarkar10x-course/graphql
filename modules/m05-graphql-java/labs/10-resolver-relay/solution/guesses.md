# Resolver relay

## Round 1: before reading INTENT.md

| Field | What I implemented, and why I read the name that way |
|---|---|
| `Club.size` | Member count including the owner: "size" of a club is its headcount |
| `Club.active` | Has any meetup at all |
| `Club.nextMeetup` | First upcoming meetup by date |
| `User.latestReview` | Newest review by createdAt |
| `User.since` | Earliest createdAt of the clubs they belong to ("member since") |
| `Book.rating` | Average rating, like `averageRating` elsewhere |
| `Book.popular` | More than 3 reviews |
| `Query.recent(n)` | Newest n reviews, newest first |

## Round 2: after INTENT.md

| Field | Matched? | Better name, type or description so nobody guesses wrong |
|---|---|---|
| `Club.size` | No | `memberCountExcludingOwner: Int!`, or keep `memberCount` and add `owner`; "size" invites guessing |
| `Club.active` | No | `hasUpcomingMeetup: Boolean!`; "active" is a business term that needs a definition |
| `Club.nextMeetup` | Yes | Fine, but say "upcoming, earliest first" in the description |
| `User.latestReview` | Yes | Fine |
| `User.since` | No | `reviewingSince: DateTime` with a description; "since" alone has no subject |
| `Book.rating` | No | `viewerRating: Int` (and an Int, since ratings are whole); per-viewer fields should say so |
| `Book.popular` | No | Keep, but the description must state the rule; better, expose `reviewCount` and let clients decide |
| `Query.recent(n)` | Yes | `recentReviews(first:)`, matching Shelfie's argument names |

## Lessons

Fields whose names are a noun plus an obvious order (`latestReview`, `nextMeetup`, `recent`) were easy;
adjectives and bare nouns (`active`, `popular`, `size`, `since`, `rating`) hid a business rule or a
viewer dependency. The schema author owes the implementer a description for every field whose value
is a rule rather than a column, and names that say whose perspective the value is from.

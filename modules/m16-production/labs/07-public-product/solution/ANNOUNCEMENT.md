# Shelfie API 2027-01: three breaking changes, and nine months to adopt them

Published: 2026-11-23
Release: 2027-01-01
2026-10 supported until: 2027-10-01

On 1 January 2027 we release version **2027-01** of the Shelfie GraphQL API. It removes two fields that have been
deprecated for months and adds a required argument to one mutation. Version 2026-10 keeps working, unchanged,
until 1 October 2027, so every client has at least nine months with both versions available.

**What changes**

- `Book.isbn10` is removed. Use `Book.isbn13`.
- `Club.memberCount` is removed. Use `Club.memberTotal`.
- `addReview` needs `visibility: ReviewVisibility!` (`PUBLIC` keeps today's behaviour).

**Who is affected.** Our usage data shows `isbn10` in no operation in the last seven days. `memberCount` is
still used by our own iOS app 2.8.0, which will be updated before the release. `addReview` is used by every
client that writes reviews.

**What to do.** Follow the [migration guide](MIGRATION.md): each change with the query before and after. Try your
operations against 2027-01 today in the sandbox, where the new version is already deployed.

**Dates**

| Date | What happens |
| --- | --- |
| 2026-11-23 | This announcement; 2027-01 available in the sandbox |
| 2027-01-01 | 2027-01 released |
| 2027-07-01 | Reminder to every client still calling 2026-10, with the operations they send |
| 2027-10-01 | 2026-10 retired: its requests are served by the oldest supported version |

Questions: reply on the developer forum thread for this release.

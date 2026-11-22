# Removing Book.isbn10: the evidence

## The field and its history

`Book.isbn10: String @deprecated(reason: "Use isbn13. ISBN-10 is not issued any more.")`. Deprecated in the
schema since 2026-06-01; `isbn13` has been there since the first release.

## Usage data

From the usage reports of the last 17 days (the 7-day replay of lab 16.2 plus the 10 days before it), queried
from the collector's raw reports:

| Window | Operations using `Book.isbn10` | Clients |
| --- | --- | --- |
| 2026-11-08 to 2026-11-15 (7 days) | 0 | none |
| 2026-10-29 to 2026-11-08 (10 days before) | 60 | iOS 2.7.0 only (`BookScreen`) |

iOS 2.8.0 and 2.9.1 send `BookScreen` without it. Over the same 7 days iOS 2.7.0 sent nothing at all, so the
release is gone, not just quiet. `Unused.java --days 7` lists the field; `--days 30` does not, which is the
honest summary: dead for a week, alive within the month.

## Decision

Remove now, because the only client that used it is a retired app version and 2.7.0 is below our minimum
supported iOS version. If 2.7.0 were still supported I would wait 30 days with zero use. `Club.memberCount` is
also deprecated, but iOS 2.8.0 used it 22 times this week, so it stays.

## What I checked

- `bin/course design diff` from the old schema to the new one: exactly one breaking change, `Book.isbn10`
  removed.
- The persisted-document list: Shelfie has none yet; with one, the field would also have to be absent from every
  stored document.
- The gap: our usage reports cover queries and mutations only. `Subscription.reviewAdded` returns `Review`, and
  `Review.book` could select `isbn10` without the reporter seeing it. No client subscribed in the replayed week
  (`Subscription.reviewAdded` is on the unused list), so I accept that risk here; with real subscribers I would
  report `subscriptionOperations` first.

## Rollback

Re-add the field (still deprecated) from the previous release; it is computed from `isbn13`, so no data is lost.

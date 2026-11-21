# Shelfie screens (Module 7 view models)

What three screens show, as the JSON a screen component wants to receive. Values are examples; the
key paths are the contract. Lab 7.1 designs a schema from these; later labs reuse them.

| File | Screen | Entry point |
|---|---|---|
| `club-page.json` | A club's page, opened from a link with the club id | a club by id |
| `book-page.json` | A book's page with the three newest reviews and the clubs reading it now | a book by id |
| `profile-page.json` | A member's profile: their clubs (with their role in each) and their shelf | a user by handle |

Rules every screen follows:

- `viewerIsMember` depends on who is signed in (`X-Shelfie-User`), not on the club alone.
- `memberPreview` is the first 5 members by join date; `latestReviews` the 3 newest.
- `role` is one of `OWNER`, `MODERATOR`, `MEMBER`; shelf `status` one of `WANT_TO_READ`, `READING`, `FINISHED`.
- Times are ISO-8601 instants (`2026-12-03T21:00:00Z`).

# Lab 4.2 notes (reference)

## addReview

Positional arguments (cannot add a field without breaking positional client code generators' assumptions,
and no single input to validate); `userId` lets any client post as anyone; `Boolean` answers neither
"which review" nor "why not". Now `createReview(input: CreateReviewInput!)` returns the review and the
book (for its new `averageRating`) plus `userErrors`.

## club_update

snake_case and noun-first; abbreviates `desc`; `private: Boolean` cannot become a third state later;
returning a bare nullable `Club` means "null = ?" (not found? forbidden?). Now `updateClub` with a patch
input (absent vs null), `visibility: Visibility`, a payload with `userErrors`.

## doMembership

A stringly-typed action is two mutations in a trench coat: different rules (owner cannot leave),
different authorisation, a `String` result. Now `joinClub` and `leaveClub`, each idempotent.

## setShelf

`userId` again; `status: String` instead of an enum; `clientMutationId` is Relay Classic legacy (clients
matched responses to requests by echo; HTTP already does that). `ShelfEntry!` with no error channel. Now
`setShelfStatus` with `ReadingStatus` and a payload.

## deleteReviews

`[ID]` allows nulls in the list; `Int` loses which ones; no errors. Now `reviewIds: [ID!]!` and
`deletedReviewIds` plus `userErrors`.

## Question

Either, stated in the description. The reference is all or nothing (one error rejects the batch), which
keeps clients simple. Partial success returns `deletedReviewIds` and a `userErrors` entry per failure with
`field: ["input", "reviewIds", "1"]`; the payload above supports both.

# Wireframe to schema: decisions (model answer)

## Decision 1: viewer-relative fields on the objects

`Club.viewerIsMember` and `Book.viewerShelfStatus` instead of `viewer { memberships ... }`: the screen
asks about *this* club, and one field keeps the club page one query. Cost: these fields cannot be
cached per object across users (Module 9).

## Decision 2: search as a union inside a results object

`SearchHit = Book | Club | User`: they share only a display name. `SearchResults { total hits }` gives
"3 results" without fetching all hits, and room for facets later.

## Decision 3: `Review.author` nullable

Accounts get deleted; reviews stay. A non-null author would erase the review, or the whole book
page under `[Review!]!`, for one deleted account (lab 3.4).

## After the model answer

(Learner's comparison.)

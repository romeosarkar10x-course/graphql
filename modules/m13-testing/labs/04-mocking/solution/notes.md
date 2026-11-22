# Lab 13.4 notes

## 1. What did the mock never show you?

Nulls. `MockWiring` fills every nullable field, so on the mock every book had an average and at least two
reviews. A screen written only against the mock has no reason to null-check `averageRating`; on the real
Shelfie, Good Omens (b5) has no reviews and `averageRating` is null, so that screen would crash there. "Zero changes" held for the code that talks to the server (the
URL came from SHELFIE_URL) but the mock had hidden a state the real data has. The fix belongs in both
places: the screen handles null, and a mock should be able to return the edge cases on purpose (named mocks,
as in MockSpec's `@mock(name: "no_ratings")`, which the mini-project builds).

## 2. Path-seeded data

Every value is a hash of the seed and the response path (`club/members/1/name`), so the same query gives the
same response byte for byte, a different seed gives different data, and two positions in a list differ.
The catch: the same book reached by two paths (`club.currentBook` and `book(id: "b1")`) gets two different
titles. Echoing `id` arguments fixes the most visible case; a real fix would key values by type and id
instead of by path, which needs an id at every level.

## 3. When to reach for WireMock instead

The digest client talks to Shelfie over HTTP, so its tests need something that speaks HTTP. WireMock stubs
are matched on the request (operation name, variables), so a test fails if the client sends the wrong
thing, which a schema-driven mock server would happily answer. Schema mocks are for building screens and
clients before the server exists; WireMock is for pinning one exact conversation with a downstream API,
including the bad days (errors with null data, W2).

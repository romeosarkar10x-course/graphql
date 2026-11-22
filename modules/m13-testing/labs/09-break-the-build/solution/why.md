# Break the build: why

| Round | Change | My call | CI (SchemaDiff) | Who is right |
|---|---|---|---|---|
| 01 | `Book.title`'s description reworded | safe | safe | both: descriptions are not part of any request or response |
| 02 | `books(first:)` default 20 to 10 | dangerous | dangerous | both: same queries, fewer books, no error anywhere |
| 03 | `Club.description` String to String! (output) | safe | safe | both: clients that handled null still work; null just stops arriving |
| 04 | `Club.name` String! to String (output) | breaking | breaking | both: clients generated their type as non-null and now meet null |
| 05 | input `rating` Int! to Int | safe | safe | both: every old request still sends it; the server must now cope with null |
| 06 | new argument `sortBy: String! = "title"` | safe | **breaking** | me: an argument with a default is not required (spec, Required Arguments), old queries validate and the resolver gets "title" (probed); graphql-inspector says dangerous |
| 07 | `Book.authors` [String!]! to [String]! | breaking | breaking | both: items can now be null inside a list clients typed as non-null items |
| 08 | argument `titleContains` renamed `title` | breaking | breaking | both: old queries now send an unknown argument and fail validation |
| 09 | type `Meetup` renamed `Meeting` | breaking | breaking | both: field shapes are the same, but `... on Meetup` fragments and generated types name the old type |
| 10 | `Book` now implements a new `Node` | dangerous | safe | debatable: nothing breaks, but a client that switches on the possible types of a new interface or caches by `Node.id` now sees Book; inspector warns |
| 11 | `UserError.field` [String!] to [String!]! | safe | safe | both: output made stricter |
| 12 | `reviewAdded(bookId:)` ID to ID! | breaking | breaking | both: a subscription without bookId (all books) is now invalid |

What fooled me first: round 06 looks like round 12 (a new non-null argument) until you see the default, and
CI makes the same mistake. Round 03 versus 04 is the output rule in one pair: tightening an output is safe,
loosening it is breaking; for inputs it is the other way round (05).

# Lab 8.4 notes

## Q1. Your filter input against a generic `where: {field, op, value}` input: what does each let a client ask for, and what does each cost you on the server?

`BookFilter` lets a client ask exactly six questions, each one an index or a cheap check:
`(published, id)` for the year range, the GIN index for search, equality on genre and author. A generic
`where` lets a client ask anything: `blurb LIKE '%a%'`, `rating != 3.2 OR pages > 10`, every column
including ones you meant to keep private. Each one has to work, stay fast, and stay supported once a
client depends on it. Even six fields don't cover every combination. On 1,000,000 rows,
`genre in (POETRY, MEMOIR) and published 1990..1999 order by title, id limit 20` took 75.7 ms: PostgreSQL
walked the `(title, id)` index in order and filtered, because no index serves that filter and that
sort together (`_workbench/2026-11-21-m08-probe/count-1m.txt`). With a generic `where`, that becomes
every query nobody planned for. The named input is also self-documenting, versionable (deprecate a
field) and type-checked. The cost: every new filter is a schema change.

## Q2. totalCount: what does it cost here, when does it run, and what would you do at 10 million rows?

It runs only when `totalCount` is in the selection set (`DataFetchingFieldSelectionSet.contains`), as
a second statement, `select count(b1_0.id) ... where <same filter>`, which has to visit every match. On
1,000,000 rows: 30 ms unfiltered (a parallel index-only scan), 25 ms for the genre and decade filter
(20,000 matches), 9 ms for a search with 6,993 matches. At 10 million rows that is roughly 300 ms
unfiltered, on every page view that shows a total. I would cap it: count up to 1,001 rows with a
`limit` subquery and return "1,000+" (a `totalCountCapped: Boolean` or a string field). Or use
PostgreSQL's estimate for unfiltered lists and cache filtered counts for a minute. The client loses
an exact number, which a search result page doesn't need.

## Q3. What did the property tests find, or what would they have found? Name one bug they catch that example tests easily miss.

They found a bug in the tests first. My first metamorphic property "added" `publishedFrom` by also
moving `publishedTo` when the range came out reversed. That widened the filter, and jqwik failed it
after 41 tries. The fix was `Assume.that(...)`, which skips those cases (324 of 1,000 checked).

This answer is an example of the shape a good answer takes. The three mutations below were not run
for the reference notes. What a learner should see:

- No `id` tie-breaker. `theFirstNAreTheModelsFirstN` fails on its first ordered case with ties, and
  the shrunk filter is usually tiny, like `BookFilter[genres=null, ...]` with `first` near 2. Hand-written
  example tests miss this, because PostgreSQL often returns ties in physical order, which is id
  order, so a three-row example passes. The harness oracle caught it in the starter: case 3 sorted by
  RATING.
- `genres: []` as "any". `countIsEveryMatch` fails as soon as it generates an empty list.
- An exclusive `publishedTo`. Found once `to` lands on a year that has books, and almost every year
  does: a boundary bug example tests rarely aim at.

## Q4. RELEVANCE ignores direction, and `genres: []` matches nothing. Defend or change one of these two decisions.

`genres: []` matching nothing is the honest reading: "books whose genre is in this set" over an empty
set is empty. A client building the list from checkboxes gets an empty result when the user unticks
everything, which is what the user asked for. A UI that wants "no genre filter" sends `null` or omits
the field. Treating `[]` as "any" makes the two indistinguishable and turns a deliberate empty
selection into the whole catalogue. The cost: client authors must know null and empty differ, so
the schema description says it. (RELEVANCE ignoring `direction` is weaker: an error for `DESC` would
be more honest than silently ignoring it.)

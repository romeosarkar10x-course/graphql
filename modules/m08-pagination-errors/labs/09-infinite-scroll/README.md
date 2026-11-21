# Game 8a — Infinite scroll

**Lecture:** [8.2 Cursor connections](../../lectures/8.2-cursor-connections.md) · **Grading:** auto · **Needs:** nothing

A feed of books, newest first, and a bot that posts a new one every second, or every 20 ms when the
judge is watching. Your client scrolls down and shows books. The one rule of infinite scroll:
**never show the same book twice, and never skip one.** The starter uses offset paging, and it
breaks the rule within two pages.

## Setup

`FeedServer.java` (in `../../contract/`) is the server. Start it in one shell:
`bin/course shell -c 'java -cp "$(source tools/testlib.sh; lib_cp)" modules/m08-pagination-errors/contract/FeedServer.java'`,
then add `--every 20 --deletes` for level 2. Its schema has `feedPage(offset, limit)` (offset paging),
`feed(first, after)` (a keyset connection, `first` 1 to 20) and `newerThan(cursor)`. `GET /stats` counts
your requests.

## Play (solo)

Write `Scroller.scroll(url, count, pause)`. It returns the ids a user sees, top to bottom, once `count`
books are on screen. It must call `pause.run()` between requests, because that is the user reading
while the bot keeps posting. The judge runs two rounds of 200 books:

| Level | Bot | You pass when |
|---|---|---|
| 1 | a new book every 20 ms | 200 shown, 0 duplicates, 0 gaps, strictly newest first |
| 2 | a new book **and a deleted one** every 20 ms | the same |

Score: **requests used** (printed). 10 is the floor for 200 books at 20 per page. Can you see why
nothing beats it?

**Bonus (not judged):** the user scrolls back to the top. Use `newerThan(cursor: <your first cursor>)`
to show a "12 new books" pill without reloading the list.

## Play (group, 2 to 6 players)

1. Everyone starts from the starter and writes a scroller in 20 minutes, without looking at each other's.
2. One person runs the server with `--every 20 --deletes`. Everyone runs the judge against it in
   turn: `bin/course test m08/09` on their own copy.
3. Leaderboard: passing scrollers ranked by requests used, ties broken by who finished first. A scroller
   that fails level 2 ranks below every passing one, whatever its request count.
4. Debrief, 5 minutes: each player says how their scroller handled a delete. Then run the
   starter at level 2 together: did it show duplicates, gaps, or both? Why?

## Done when

`bin/course test m08/09` passes both levels.

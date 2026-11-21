# Mini-project 8 — A terminal book browser

**Lecture:** [8.2 Cursor connections](../../lectures/8.2-cursor-connections.md) · **Grading:** auto · **Needs:** nothing

A full-screen terminal app in Java ([Lanterna](https://github.com/mabe02/lanterna) 3.1.5) that pages
through the feed with cursors as you move with the arrow keys: the client side of a connection.
It loads the next page *before* you reach the end, never shows a book twice while new ones arrive on
top, and never asks for a page it already has.

The server is game 8a's `FeedServer.java`. Start it with
`bin/course shell -c 'java -cp "$(source tools/testlib.sh; lib_cp)" modules/m08-pagination-errors/contract/FeedServer.java --every 2000'`,
then run the app from your copy in a second shell:
`bin/course shell -c 'mvn -q package && java -jar target/book-browser.jar'`.

## The screen (the test reads it back)

On a terminal `h` rows tall:

- Row 0: `Shelfie books  N loaded`. Anything may follow, such as the starter's `  (end)`.
- Rows 1 to `h-2`: one book per row, `#id title - author`. The selected row starts with `> `, the
  others with two spaces.
- Row `h-1`: `App.FOOTER`.

## Do

The starter draws the screen, loads the first page, and moves with Up and Down. `Browser` is yours:

1. **Load ahead, by cursor.** When the selection gets within 5 rows of the last loaded book, load the
   next 20 with `feed(first: 20, after: endCursor)`. Stop when `hasNextPage` is false. 200 books must
   take at most 11 requests.
2. **Keys.** PgDn and PgUp move by one screen of rows (13 on the test's 60x15 terminal). Home goes to
   the top. Up at the top stays put. PgDn at the end stops on the last book.
3. **Scrolling.** The selected row is always visible. Moving past the bottom row scrolls the list, so
   after 16 Downs the 17th book is on the last list row.
4. `q` returns from `App.run`.

Keep `Browser` free of Lanterna: it is plain state, which is what makes it easy to reason about. The test
drives the real app on Lanterna's `DefaultVirtualTerminal`. Keep the starter's `pollInput()` loop:
3.1.5's virtual terminal holds a lock while `readInput()` waits, and the test could not read the screen.

## Group variant (pairs)

1. One person writes `Browser` (state and paging), the other the key loop and the drawing in `App`,
   agreeing only on `Browser`'s public methods first. 30 minutes.
2. Swap and review: each reads the other's half against the four rules above and files one
   issue. Fix the issues together.
3. Run the test against the live feed, then explain to each other why the list never repeats while
   the bot posts.

## Done when

`bin/course test m08/11` passes: 14 checks on a fixed feed of 200 books, and 4 while a bot adds a book
every 30 ms.

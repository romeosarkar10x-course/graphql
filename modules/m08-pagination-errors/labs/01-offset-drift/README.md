# Lab 8.1 — Offset paging, and the list that moves

**Lecture:** [8.1 Offset pagination and its limits](../../lectures/8.1-offset-pagination.md) · **Grading:** both · **Needs:** postgres (`bin/course ref up`)

`books(page: 2, size: 20)` is the pagination everyone writes first, and Spring Data makes it one line.
It works until the list changes while someone is scrolling, or until someone asks for page 49,999.
You build it properly, then measure both failure modes.

The app reads `m08_01.book`, 10,000 books from the course catalogue, newest first (`addedAt` descending,
then `id` descending). The test reloads it before each run. To run the app yourself, load it with
`bin/course catalog m08_01` and run
`bin/course shell -c 'mvn -q package -DskipTests && java -jar target/shelfie-offset.jar'` from your copy.

## Do

1. `books(page, size)`: page in the database with `findAll(PageRequest.of(page, size, NEWEST_FIRST))`,
   not in Java. The starter loads all 10,000 rows per request. Fill `BookPage` from Spring Data's
   `Page`: `totalCount`, `totalPages`, `hasNextPage`.
2. Reject `page < 0`, and `size` outside 1 to 50, with `extensions.classification: BAD_REQUEST`. A page
   past the end is just empty.
3. `addBook(title)` saves a book with `addedAt = now`, so it lands on top of page 0. `deleteBook(id)`
   returns `true` once and `false` after that.

## Experiment

With the app running, run `Drift.java` from a second shell:
`java -cp "$(source /course/tools/testlib.sh; lib_cp)" Drift.java insert`, then the same with `delete`.
It scrolls 5 pages of 20. After each page someone adds 3 books (or deletes 3 you've already seen).
It prints what you saw twice and what you never saw. The test runs it too and expects exactly 12
each time. Then load a million rows (`bin/course catalog m08_01 1000000`, restart the app) and time
page 0, 500, 5,000 and 49,999 with `curl -w '%{time_total}'`. Look in the log for every SQL statement a
single `books` request runs. Answer the questions in `notes.md`.

## Done when

`bin/course test m08/01` is green and `bin/course grade m08/01` passes the notes.

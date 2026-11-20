# Lab 3.7 — A feed as a sealed union

**Lecture:** [3.7 Unions and Java sealed types](../../lectures/3.7-unions-sealed.md) · **Grading:** auto + rubric · **Needs:** nothing

A club feed mixes four kinds of event that share almost nothing. In GraphQL that is a union; in Java 21+
it is a sealed interface of records. Together with a `switch` that has no `default`, the compiler tells
you every place a new event type must be handled, before a client ever sees it.

## Do

1. **TODO 1:** make `FeedItem` a `sealed interface`, with no `permits` clause (the records live in the
   same file, so they are permitted implicitly).
2. **TODO 2:** rewrite the type resolver as a pattern-matching `switch` over `FeedItem`, no `default`.
   (The starter's `if` chain sends `BookFinished` to the wrong type. Run the c1 feed and see.)
3. **TODO 3:** wire `BookFinished.book`.
4. Keep the `// -- new feed items go here --` line: the test adds a `ClubRenamed` record there and
   expects your code to **stop compiling**.
5. `notes.md`.

## Done when

`bin/course test m03/07` is green and `bin/course grade m03/07` gives 7/10 or more.

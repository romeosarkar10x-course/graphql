# Lab 6.5 — Generated fetchers, honestly compared

**Lecture:** [6.5 Spring Data integration](../../lectures/6.5-spring-data.md) · **Grading:** auto + rubric · **Needs:** nothing

A separate small app (`shelfie-data`): Shelfie's ten books in H2 through JPA, behind `books.graphqls`.

1. Make Spring for GraphQL **generate** `Query.book` and `Query.books` from `BookRepository`
   (`@GraphQlRepository`, Query by Example). No controller methods for them.
2. Write `Query.searchBooks` **by hand**: a derived query, case-insensitive, ordered by year.
3. Turn on SQL logging and compare what each kind of fetcher sends to the database (`notes.md`).

`tests/lab.hurl` checks all three fields; the test also checks that `book`/`books` have no controller.

Not covered: **Querydsl**. Spring for GraphQL supports it the same way (`QuerydslPredicateExecutor`), but
it needs an annotation processor generating Q-classes, and the original `com.querydsl` 5.1.0 has seen no
release since 2024 (the maintained fork is `io.github.openfeign.querydsl`). Query by Example shows the
same trade-off with no build plugin. The plan's **projected interfaces** (`@ProjectedPayload`) bind
arguments to Spring Data projections; they are mentioned in the lecture, not built.

## Done when

`bin/course test m06/05` is green and `bin/course grade m06/05` gives 7/10 or more.

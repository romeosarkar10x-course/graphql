# Lab 8.4 — Filters, sort orders and search

**Lecture:** [8.3 Filtering, sorting, search](../../lectures/8.3-filtering-sorting-search.md) · **Grading:** both · **Needs:** postgres (`bin/course ref up`)

The schema is written and it is the contract: a `BookFilter` input with six named fields, a sort
enum, `first` capped at 100, and `totalCount`. No generic `where` input, no free-form `orderBy: String`.
Your job is the server behind it: JPA Specifications built from the filter record, PostgreSQL full-text
search, a total order, the error rules, and property tests that try 1,000 random filters per property
to prove it.

The app reads `m08_04.book` (10,000 books); `schema.sql` adds two SQL functions, `book_matches` and
`book_rank`, around `to_tsvector`/`websearch_to_tsquery`. To run the app yourself: `bin/course catalog m08_04`,
then `bin/course shell -c 'mvn -q package -DskipTests && java -jar target/shelfie-filters.jar'`.

## Do

1. `BookSpecs.of(filter)`: one `Specification` per non-null field, combined with `Specification.allOf`.
   `genres: []` matches nothing, while `genres: null` matches everything. `search` calls `book_matches`
   with `cb.function`, and the search text must be a **bind parameter**: the test searches for a
   word and fails if it shows up inside the logged SQL. Find out what `cb.literal` does here.
2. `BookService.find`: sort by the field, then **always** by `id` ascending. 460 titles, 125 years
   and 41 ratings for 10,000 books means ties on almost every page. `RELEVANCE` orders by
   `book_rank(...)` descending, then `id`, and ignores `direction`.
3. `validate`: each of these is a `BadFilterException`, which the controller turns into `BAD_REQUEST`:
   `first` outside 0 to 100, `publishedFrom` after `publishedTo`, `minRating` outside 1.0 to 5.0, a
   blank `search`, and `RELEVANCE` without a search.
4. `FilterPropertiesTest` (jqwik 1.10.2, `mvn test`): at least **3 properties with
   `@Property(tries = 1000)`**. Compare `service.find` and `service.count` with a plain-Java model over
   `all`, for random filters on every field except `search` and every order except `RELEVANCE`. At
   least one property should be *metamorphic*, needing no model: adding a filter field never grows
   the count. The starter has one example property with 100 tries.

The test also runs its own oracle: 300 seeded random cases over HTTP, including search and RELEVANCE,
checked against Java and PostgreSQL directly.

## Experiment

Break your solution on purpose, three ways, one at a time: drop the `id` tie-breaker, make `genres: []`
mean "any genre", and make `publishedTo` exclusive. For each one, note whether your properties
catch it, how many tries it took, and what jqwik shrank the failing filter to. Then answer the notes.

## Done when

`bin/course test m08/04` is green and `bin/course grade m08/04` passes the notes.

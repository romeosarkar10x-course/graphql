# Lab 15.2: Stitching and hydration with Nadel

Lecture: [15.2 Stitching and hydration](../../lectures/15.2-hydration.md). Grading: auto + rubric. Needs nothing.

Two plain GraphQL services that know nothing of each other
([contract/plain/PlainService.java](../../contract/plain/PlainService.java)):

- **catalog** on :4101: `book(id:)`, `books(ids:)` (the batch field), `allBooks`; `Book { id title authors pages }`.
- **reviews** on :4102: `review(id:)`, `latestReviews(first:)`; a `Review` has only a `bookId`.

Build the gateway in front of them with **Nadel**, Atlassian's distributed GraphQL engine (a Kotlin library on the
JVM, Maven Central `com.atlassian:nadel`). The starter is a runnable Java gateway: Nadel built from the `.nadel`
files (the API clients see) and `underlying/` (each service's own schema), an HTTP endpoint on :4000, and a
`--schemas=<dir>` option to load the schema files from a directory instead of the jar. Two things are missing:

1. `ServiceExecution`: how Nadel calls a service. Print the document it gives you, POST it with its variables,
   return the service's `data` and `errors`.
2. `Review.book: Book` in `reviews.nadel`, **hydrated** from catalog with `@hydrated`, in batches: five reviews
   are one call to catalog. `bookId` must not be visible to clients.

The grader runs the two services and your jar, checks the answers and the call counts, then does the
experiment: it copies your schema files, renames `Book.title` to `name` **at the gateway** with
`@renamed(from: "title")`, and starts your jar with `--schemas=` on the copy.

```bash
bin/course start m15/02
cd workspace/m15-federation/02-hydration
bin/course shell -c 'bash /course/modules/m15-federation/contract/plain/start.sh && mvn -q package && java -jar target/shelfie-nadel.jar'
bin/course test m15/02
```

`GET :4101/_stats` shows the documents Nadel sent to catalog. In `notes.md`: who owns `Review.book`, who decides
a rename, and stitching against federation.

## Done when

`bin/course test m15/02` passes and `notes.md` passes the rubric (`bin/course grade m15/02`).

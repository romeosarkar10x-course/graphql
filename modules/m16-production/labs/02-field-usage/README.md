# Lab 16.2: Field usage: Hive-format reports, and the fields nobody uses

Lecture: [16.2 Registry and field analytics](../../lectures/16.2-registry-usage.md). Grading: auto.

A schema registry (Hive, GraphOS, Cosmo) can tell you which fields anyone still uses. To do that it needs
usage reports from your server. Implement `UsageReporter`, a graphql-java `Instrumentation`, so that it sends
**Hive's usage report format, version 2**. The course's stand-in for Hive's endpoint is
`contract/UsageCollector.java` on :4200. It checks the same shape and keeps what you send.

```json
{
  "size": 1,
  "map": {
    "3f2a…": {"operation": "query Shelf { books(first: 2) { title } }", "operationName": "Shelf",
              "fields": ["Query.books", "Query.books.first", "Book.title"]}
  },
  "operations": [
    {"operationMapKey": "3f2a…", "timestamp": 1795370000000,
     "execution": {"ok": true, "duration": 2400000, "errorsTotal": 0},
     "metadata": {"client": {"name": "android", "version": "1.4.0"}}}
  ]
}
```

- `fields`: each field coordinate the operation selects (`Type.field`), and each argument it passes
  (`Type.field.arg`). Leave out `__typename`. Hive also lists type names and more; you do not have to.
- `timestamp`: milliseconds since the epoch. When the request has an `X-Simulated-Time` header, use that header's
  value. `Observability` has already put it in the GraphQL context.
- `duration`: nanoseconds. `ok`: no errors. `errorsTotal`: how many errors.
- `client`: the raw `graphql-client-name` and `graphql-client-version` headers, from the context.
- A document that does not parse or validate selects nothing: do not report it. Hive needs at least one field.
- Batch: POST to `shelfie.usage.endpoint` at least once a second (and sooner when 100 operations are waiting). Send
  the headers `X-Usage-API-Version: 2` and `Authorization: Bearer <shelfie.usage.token>`. The course token is
  `course-usage-token`.

Then **Unused.java**, a small program: read `GET :4200/_reports` and the schema, and print every field coordinate
(`Type.field`) that no operation used in a time window, sorted, one per line. The test replays seven simulated
days (`contract/Traffic.java`: 1,460 operations from web, iOS, Android and a partner, timestamps up to
2026-11-15) and runs:

```bash
bin/course java Unused.java src/main/resources/graphql/shelfie.graphqls --now 2026-11-15T00:00:00Z --days 7
```

```bash
bin/course start m16/02
cd workspace/m16-production/02-field-usage
bin/course test m16/02
```

## Experiment: what "unused" means

Look at the list. One entry is not dead at all: nothing in the replay could have used it. Which one, and why?
The next lab removes a field on the strength of this list. Before it does, decide what other evidence you would
want, and over what window.

## Done when

`bin/course test m16/02` passes.

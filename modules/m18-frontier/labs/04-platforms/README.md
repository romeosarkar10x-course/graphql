# Lab 18.4: An API you did not write

Lecture: [18.4 Platforms beyond hand-built servers](../../lectures/18.4-platforms.md). Grading: auto + rubric.
Needs PostgreSQL, Node and internet once (npm).

Seventeen modules built Shelfie's API by hand. Some teams never write one: they point a tool at the database
and get a GraphQL API generated from tables, foreign keys and functions. This lab does it with
[PostGraphile](https://postgraphile.org/) 5.2.1 (MIT) on Module 9's club dataset (100 clubs, 2,000 readers,
2,000 books, 20,000 reviews), which the test loads into the schema `m18_04`.

The starter's `graphile.config.mjs` is PostGraphile's "amber" preset with nothing changed. Run it and look:

```bash
bin/course start m18/04
cd workspace/m18-frontier/04-platforms
bin/course shell -c 'source /course/tools/testlib.sh; java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java m18_04'
COURSE_PUBLISH=5678 bin/course node sh -c 'npm ci && npm start'      # Ruru (its GraphiQL) at http://localhost:5678/
bin/course test m18/04
```

Then make it fit to face clients, in the config only (no SQL changes):

1. **Read-only.** The amber preset generates create, update and delete mutations for every table, including
   `deleteReader`. Remove them all: writes go through a service that checks who is asking.
2. **No plumbing.** The loader's helper function `pick(words, g, k)` became a Query field. Remove it from the API.

The checks (`tests/generated.hurl`) query the API as generated: `clubByRowId`, `readerByOwnerId`,
`membershipsByClubId(orderBy: JOINED_AT_ASC)`, connections with `totalCount`, cursors and `node(id:)`.

**Then write `GAPS.md`:** what you would still have to build by hand before this API faced Shelfie's clients.
Compare it with the hand-built schema of Modules 7 to 10 (names, nullability, errors, authorization, demand
control, persisted documents) and with at least one other platform from the lecture: Hasura, Supabase's
pg_graphql, the Neo4j GraphQL Library, Dgraph, or Airbnb's [Viaduct](https://github.com/airbnb/viaduct) (JVM,
Apache-2.0, v3.0.0 of 2026-10-21). Viaduct's starter is Gradle-based upstream; run it on your machine with its own
wrapper, as its README says:

```bash
git clone https://github.com/viaduct-dev/cli-starter.git && cd cli-starter
./gradlew -q run --args="'{ greeting }'"
```

(The course did not run that build: the authoring session was not allowed to run a Gradle wrapper. The command
is copied from Viaduct's README at v3.0.0; record what happened on your machine.)

## Done when

`bin/course test m18/04` passes (the API is read-only, `pick` is gone, the seven checks hold) and `GAPS.md` passes
the rubric (`bin/course grade m18/04`).

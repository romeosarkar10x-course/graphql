# Bake-off: one feature, four stacks

The feature: the club card, `{ club(id: "c1") { name memberCount owner { name } currentBook { title } } }`.

## Numbers (median of 3 rounds of `./bake.sh`)

| Stack | Lines | Startup ms | RSS MB | p50 ms | p95 ms | req/s |
|---|---|---|---|---|---|---|
| raw graphql-java | | | | | | |
| Spring for GraphQL | | | | | | |
| DGS | | | | | | |
| Quarkus | | | | | | |

## Test ergonomics

How would you test the club card in each stack? Name the tool and say how long a test takes to run.

## Verdict

Which stack for Shelfie, and why? Which numbers above would change your mind, and by how much?

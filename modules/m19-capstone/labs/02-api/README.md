# m19/02 Stage 2: a working API

About 7 hours. The services that serve the contract, with the core operations working against real storage,
and the Hurl suite green.

```bash
bin/course start m19/02          # same repository as stage 1
```

## Do

1. **Build the services** in Java with Maven: Spring for GraphQL is the default (DGS or Quarkus are fine if you
   argue it in an ADR). Load the schema from `contract/` (a Maven resource pointing at it), not a copy.
2. **Storage:** PostgreSQL at `postgres:5432` (user and password `shelfie`), in a database schema of your own
   per subgraph. Load a seed at start, so every run starts from known data.
3. **The core operations:** the queries behind the main screens, cursor pagination with filter and sort, and the
   main mutations with their typed failures and an idempotency key where a retry could do harm.
4. **The manifest:** `build`, `run` (each process, with a `ready` URL), `api`, `send: "query"` for now, each
   subgraph's `url`, and three probes: `pagination`, `typedError`, `idempotent`
   ([contract/MANIFEST.md](../../contract/MANIFEST.md)).
5. **The Hurl suite** green against your running stack.

## Check

```bash
bin/course test m19/02    # builds, starts everything, compares the served schema with the contract, runs the
                          # Hurl suite and the three probes, then stops everything
bin/course grade m19/02
```

By hand: `bin/course shell`, start your processes, then `hurl --test --variable api=... tests/api.hurl`.

## Done when

Green, and the rubric's correctness rows are at full marks: the suite tests behaviour (failures, edges,
pagination ends), not only the happy path.

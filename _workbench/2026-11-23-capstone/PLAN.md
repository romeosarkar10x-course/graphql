# Capstone build plan (2026-11-23)

The user said "let's go" after the Module 18 report. Supported host: Linux only.

## Shape

`modules/m19-capstone/`, so the runner's `mNN/NN` ids work (`m19/01` ... `m19/05`). It is called "Capstone",
not a 20th module, everywhere a learner reads it.

- One learner repo across all stages: lab.json `"workspace": "m19-capstone/repo"` (runner change). `start` keeps
  an existing shared workspace.
- One reference product, **Toolshed** (a community tool library), deliberately not one of the five briefs, in
  `modules/m19-capstone/reference/`; each stage's `solution` is a symlink to it. Starter: a template repo
  (manifest skeleton, doc templates); every stage's starter is that template.
- The learner's product is unknown, so the checker is generic: `capstone.json` (the manifest) says where the
  schema, operations, Hurl suite, docs are, how to build and run, which tokens, and the probes (pagination,
  typed error, idempotency, auth, cost, N+1 counter, real-time, cross-subgraph). The checker runs them and the
  rubric judges their quality.

## Stages (labs)

1. `m19/01` pitch, design, contract (both): schema analysis (types >= 15, interface with 2+ implementations,
   union, `@oneOf` input, connection with filter and sort, typed-error mutation, subscription or `@defer`),
   operations validate, Hurl suite present, design doc sections, ADRs, pitch. Rubric: Module 7 review.
2. `m19/02` working API (both): build, run, served schema equals the contract, Hurl green, pagination,
   typed error, idempotency probes.
3. `m19/03` hardening (both): auth probes with course tokens, untrusted document refused, persisted documents
   work, expensive operation refused, N+1 counter flat across page sizes, real-time event arrives, Java client
   runs, UI present.
4. `m19/04` federation and ops (both): 2+ subgraphs compose, router serves, cross-subgraph trace in Tempo with
   2+ services, Grafana dashboard loads and its queries return data, runbook, CI workflow gates.
5. `m19/05` demo day (rubric): the syllabus's capstone rubric, demo script, Q&A, attack report.

## Reference architecture (Toolshed)

- `inventory` subgraph :4001 (tools, categories, branches; `Tool` interface, `@oneOf` lookup, search union,
  connections), `lending` subgraph :4002 (members, loans, borrow/return with result unions and idempotency
  keys, availability subscription), Hive Router :4000 (persisted documents from a file, demand control, depth
  limit, Authorization propagated).
- Spring Boot 4.2.0, federation-jvm 7.0.0, Spring Security resource server with the course key (Module 10),
  JDBC on the course PostgreSQL, `@BatchMapping`, Micrometer + OTLP, Java client (Spring `HttpSyncGraphQlClient`),
  a static HTML UI.

# Capstone: build a new product from an empty repository

About 25 hours. One product of your own, in Java, from an empty repository to a demo: every design decision is
yours, and every requirement below is checked, by the course checker where a machine can tell and by a rubric
where it takes judgement. It is 35% of the course grade.

Supported host: Linux (the course is tested on Arch Linux); everything runs in the course image as usual.

## How it works

- **One repository for all stages.** `bin/course start m19/01` creates `workspace/m19-capstone/capstone/` from a
  template (a manifest, document templates, empty folders). Stages 2 to 5 work on the same repository:
  `bin/course start m19/02` and later only point you back to it.
- **The manifest.** The course cannot know your product, so `capstone.json` tells the checker where your schema,
  operations, Hurl suite and documents are, how to build and run your services, which course tokens to mint,
  and which requests to send to see your API behave ("probes"). Every key: [contract/MANIFEST.md](contract/MANIFEST.md).
- **Stages are checked in order** (`bin/course test m19/01` ... `m19/04`) and graded by rubric
  (`bin/course grade m19/NN`). A later stage's checks start your stack fresh and do not re-check earlier
  stages' documents, so keep them green yourself.
- **The reference product**, Toolshed (a community tool library), is in `reference/`. It passes every stage
  and is not one of the briefs, on purpose. Graders read it; you should not need to.

## Pick a brief

| Brief | Domain | Why it stretches you |
| --- | --- | --- |
| Event ticketing | Venues, events, seat holds, orders, a live seat map | Concurrency on seat holds, real-time availability, payment results as typed errors |
| Multiplayer quiz platform | Rooms, questions, answers, live leaderboards | Subscriptions at scale, anti-cheat authorization, `@defer` for results |
| Food delivery | Restaurants, menus, carts, orders, courier tracking | Federation across 3+ domains, location streams, heavy reads |
| Team task tracker | Workspaces, projects, tasks, comments, activity feed | Deep authorization, multi-tenancy, polymorphic feed |
| Your own idea | Approved via a one-page pitch | Must meet the requirement list below |

Or one of the capstone-grade ideas in [plan/projects-and-games.md](../../plan/projects-and-games.md) (live
auction house, transit tracker from GTFS feeds, recipe graph, AI study buddy over MCP, ...). Your own idea is
approved by the stage 1 review: the pitch has to show that the product can meet every requirement.

## Required in every capstone, and where it is checked

| Requirement | Stage | Checked by |
| --- | --- | --- |
| Design doc: schema, architecture diagram, nullability and error strategy, auth model, ADRs | 1 | headings, words, a diagram, 3+ ADRs; the design review |
| Contract first: `.graphqls` schema, operations folder and a Hurl conformance suite | 1, 2 | the contract composes, operations validate, the running services serve exactly the contract, Hurl green |
| At least 15 types including an interface, a union (sealed Java types) and a `@oneOf` input | 1, 3 | schema analysis; `sealed interface` in the code |
| Cursor connections with filtering and sorting | 1, 2 | a connection field with first/after, a filter input and a sort argument; the pagination probe |
| Mutations with typed errors and idempotency where it matters | 1, 2 | a mutation result that carries failures as data; the typed-error and idempotency probes |
| One real-time feature (subscription or `@defer`) | 1, 3 | a real-time operation; an event arrives after a trigger |
| DataLoader or `@BatchMapping`; the N+1 detector green in CI | 3, 4 | the N+1 probe on your statement counter; a test that counts statements |
| Spring Security auth, field-level authorization, cost limits, trusted documents | 3 | auth probes with course tokens, an unknown document refused, an expensive request refused |
| A Java client (Spring `GraphQlClient` or DGS codegen client) plus one UI of any kind | 3 | the client runs; the UI exists (the rubric looks at both) |
| CI with unit, `GraphQlTester`, Testcontainers, schema-diff and PIT gates | 4 | the workflow and the tests are read (not run) |
| At least 2 subgraphs composed behind a router | 4 | 2+ subgraphs; the served schemas compose; the suite runs through the router |
| Micrometer/OpenTelemetry tracing, a Grafana dashboard and a runbook | 4 | one trace across 2+ services in Tempo; the dashboard's panels show data in Grafana; runbook alerts |
| A 10-minute demo and live Q&A | 5 | rubric |

## Stages

| Lab | Stage | About | Grading |
| --- | --- | --- | --- |
| [m19/01](labs/01-design/README.md) | 1. Pitch, design doc and contract, peer-reviewed | 5 h | auto + rubric |
| [m19/02](labs/02-api/README.md) | 2. A working API with the core operations, Hurl suite green | 7 h | auto + rubric |
| [m19/03](labs/03-hardening/README.md) | 3. Client, real time, performance and security hardening | 7 h | auto + rubric |
| [m19/04](labs/04-federation-ops/README.md) | 4. Federation, observability and CI | 5 h | auto + rubric |
| [m19/05](labs/05-demo-day/README.md) | 5. Demo day: 10 minutes and Q&A, and the capstone rubric | 1 h | rubric |

`bin/course checkpoint 19` checks stages 1 to 4 and the demo script, then tags your workspace.

## Solo and group

- **Solo:** the stage 1 review is done by the AI grader acting as your peer (`bin/course grade m19/01`), and
  demo day is recorded (a screen recording and its transcript) and graded the same way.
- **Group (2 or 3 people), one of two ways:**
  - *Separate products, swapped reviews:* each person builds their own capstone; at stage 1 you review each
    other's contract against [contract/design-review.md](contract/design-review.md), and demo day is live,
    with the others asking the questions.
  - *One product, one subgraph each:* a shared repository, each person owns one subgraph end to end (schema,
    service, tests, the ADRs about it), with one router. The checker is run on the shared repository; the
    rubric is scored per person for their subgraph and jointly for the rest. Agree the entity boundaries in the
    stage 1 review before anyone writes code.
  - To run one person's stack where the others can reach it: `COURSE_PUBLISH=4000 bin/course shell`, then start
    the router with `HOST=0.0.0.0`.

## Grading

The capstone rubric (stage 5, [labs/05-demo-day/rubric.md](labs/05-demo-day/rubric.md)) is the syllabus's:
schema design 20%, correctness and tests 15%, performance 15%, security 15%, Java craft 10%, architecture and
ops 15%, communication 10%. The stage checks are the floor: a stage that does not pass its checks cannot score
above half marks in the areas it covers. Stage rubrics give feedback on the way and feed the final score.

## Shorter tracks

The syllabus's tracks change the capstone's focus. The Spring fast track and the Java backend track do stages
1 to 3 with one service (`subgraphs` lists one; `api` is the service itself) and skip stage 4's federation
checks; the API consumer track builds a typed Java client and CLI against a public API instead and is graded by
rubric only; the platform track does everything.

## What the checker needs from you

Your services run inside the course image (PostgreSQL is `postgres:5432`, user and password `shelfie`; use your
own database schema). Traces go over OTLP/HTTP to `http://localhost:4318/v1/traces`. Avoid ports 3000, 3200,
4317, 4318 and 9090 (stage 4 runs Grafana, Tempo and Prometheus there). Tokens come from the course's identity
setup (Module 10): `bin/course token <user> --aud <your-api>`, verified offline with
`modules/m10-security/contract/keys/course-public.pem`.

## Changes from the plan

- The syllabus lists the capstone without a module number; it is `modules/m19-capstone` so the runner's lab ids
  work (`m19/01`). It has no lectures: the modules are the lectures.
- The syllabus's stage 1 says "peer-reviewed against the Module 7 rubric". Module 7 has one rubric per lab, not one
  for a whole design, so [contract/design-review.md](contract/design-review.md) collects Module 7's questions
  (plus errors, pagination, authorization and boundaries from Modules 4, 8, 10 and 15) into one.
- Stage 5 (demo day) is a lab of its own, so the demo and the final rubric have a place.
- The checker reads CI workflows and test sources; it does not run them (as in Module 13). It does run your
  services, your Hurl suite, the probes, Tempo, Prometheus and Grafana.
- Real-time checks use server-sent events (Accept: text/event-stream) at your API, which Spring for GraphQL
  and Hive Router both speak, or graphql-transport-ws when `realtime.url` is a `ws://` URL (Module 11's client).

If you compose with Rover (`bin/course rover`), declare `directive @oneOf on INPUT_OBJECT` in each subgraph
that uses it: Apollo's composition does not define it (it then drops it from the supergraph; your subgraph still
enforces it). The course's composer needs no declaration.

## Reference product notes

Toolshed's build is described in [reference/README.md](reference/README.md) and its own documents. The
course's probe output for it is in `_workbench/2026-11-23-capstone/`.

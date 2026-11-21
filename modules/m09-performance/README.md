# Module 9 — Performance and data loading in Java

About 9 hours, 8 lectures. By the end you can find and kill N+1 queries, pick the right cache, and prove
every gain with numbers.

## Before you start

`bin/course ref up` now also starts **Redis 8.8.4** (`redis:6379`) and **Varnish 9.1.0** (`varnish:6081`
inside the course image, `localhost:6081` from the host). PostgreSQL is still there from Module 8.

- `bin/course clubs <schema> [scale]` loads the club dataset (`contract/ClubData.java`): 100 clubs, 2,000
  readers, 2,000 books, 25 members per club, 10 reviews per reader. Every row is a function of its id,
  so statement counts and responses are exact. Each lab has its own schema (`m09_01` ...) and its test
  reloads it.
- Every app serves `contract/club.graphqls`. The yardstick is `contract/dashboard.graphql`: 10 clubs, 20
  members each, 5 reviews per member, each review's book. `contract/dashboard.expected.json` is its
  response, and every lab must keep it byte for byte.
- Varnish starts with no backend. A test points it at the app in its own container with
  `python3 /course/tools/varnish.py 4000`.

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 9.1 | [Seeing N+1](lectures/9.1-seeing-n-plus-one.md) | [m09/01 Count every statement](labs/01-seeing-n-plus-one/README.md) | auto + rubric |
| 9.2 | [DataLoader from scratch](lectures/9.2-dataloader-from-scratch.md) | [m09/02 A DataLoader from scratch](labs/02-dataloader-from-scratch/README.md) | auto |
| 9.3 | [java-dataloader and Spring](lectures/9.3-java-dataloader-spring.md) | [m09/03 Batch loading](labs/03-batch-loading/README.md) | auto + rubric |
| 9.4 | [Lookahead and smarter SQL](lectures/9.4-lookahead-sql.md) | [m09/04 One SQL statement](labs/04-lookahead-sql/README.md) | auto + rubric |
| 9.5 | [Caching layers](lectures/9.5-caching-layers.md) | [m09/05 Four layers, five stale-data bugs](labs/05-caching-layers/README.md) | auto + rubric |
| 9.6 | [Persisted queries for speed](lectures/9.6-persisted-queries.md) | [m09/06 APQ over GET, behind Varnish](labs/06-persisted-queries/README.md) | auto + rubric |
| 9.7 | [Measuring properly](lectures/9.7-measuring.md) | [m09/07 JMH, Gatling, JFR, async-profiler, Micrometer](labs/07-measuring/README.md) | auto + rubric |
| 9.8 | [Engine and JVM tuning](lectures/9.8-engine-jvm-tuning.md) | [m09/08 Four configurations, one winner](labs/08-tuning/README.md) | auto + rubric |

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m09/09 Performance race](labs/09-performance-race/README.md) | 9 or fewer statements and half the naive p95, judged against a baseline in the same run | 45 minutes, two leaderboards (statements, p95), debrief by each winner |
| [m09/10 Guess the query count](labs/10-guess-the-query-count/README.md) | 10 rounds, 14 of 20 to pass; misses explained with the counter | The instructor shows each query, everyone guesses on paper, the counter reveals it |
| [m09/11 N+1 detector](labs/11-project-n-plus-one-detector/README.md) (mini-project) | A JUnit 5 extension that fails tests over their statement budget, plus a CI workflow | Pairs: one writes the extension, the other a hidden N+1 for it to catch |

## Changes from the plan

- **9.2's "hidden test suite"** is in `tests/`, readable; the README asks you to write your own tests
  first. Its failure rule follows java-dataloader 6.0.0, which doesn't remember failed keys
  (`_workbench/2026-11-21-m09-probe/failcache.txt`).
- **9.3** adds `Club.ownerPicks`, a chained loader, to the contract, because the probes found that chained
  loaders hang under graphql-java's default dispatch and that Spring never turns chaining on.
- **9.4 uses jOOQ `MULTISET`** (not JPA entity graphs) and reports that one statement was slower than
  eight on this data.
- **9.5's five bugs are built in**: the starter is an app with all four layers and one bug per scenario,
  and the experiment is finding the guilty layer. Redis and Varnish are course services.
- **9.6 measures behind Varnish 9.1** in the course network. `varnish.py` speaks Varnish's CLI to load the
  backend at test time, because the app's address is only known then.
- **9.7 uses Gatling** (not k6), with async-profiler in `itimer` mode from its Maven Central jar; no image
  change was needed.
- **9.8's four configurations** are platform or virtual threads with G1 or ZGC. Reactor is discussed in
  the lecture, not measured.
- The N+1 detector reads the app's own datasource-proxy counter (`SqlCounter`), which records SQL text
  while a test runs.

## Spec references

None required: performance is implementation-defined. See
[Execution](https://spec.graphql.org/September2025/#sec-Execution) for what must stay observable: a
batched or cached response must be the response the spec's algorithm would produce.

## Checkpoint

`bin/course checkpoint 09` (see [checkpoint.md](checkpoint.md)): loaders everywhere (labs 9.1 to 9.8),
the N+1 detector and its CI workflow, and the performance report committed. Tag: `checkpoint-09`.

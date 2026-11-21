# Lab 9.7 — Measuring properly: JMH, Gatling, JFR, async-profiler, Micrometer

**Lecture:** [9.7 Measuring properly](../../lectures/9.7-measuring.md) · **Grading:** both · **Needs:** postgres

You'll prove lab 9.3's gain with numbers you'd defend in a review. The starter has four parts:

| Part | What | Your job |
|---|---|---|
| `app/` | the **naive** app (lab 9.1) | measure it, then bring in your loaders from lab 9.3 and measure again; add Micrometer percentiles |
| `jmh/` | a JMH project (1.37) | write `DocumentBench`: what does parsing and validating cost per request? |
| `load/` | a Gatling project (3.16.1, Java DSL) | write `DashboardSimulation`: an open workload with assertions |
| `measure.sh` | the harness that runs them together | read it; run it twice |

## Do

1. **Micro benchmark.** In `DocumentBench`, build two `GraphQL` instances on `contract/club.graphqls`
   with no data fetchers: one plain, one with a `PreparsedDocumentProvider` that caches by query text.
   Benchmark executing `contract/dashboard.graphql` on each. Run:
   `bin/course shell -c 'cd jmh && mvn -q package && java -jar target/benchmarks.jar'`.
2. **Load test.** `DashboardSimulation` posts the dashboard with `injectOpen(constantUsersPerSec(rate).during(...))`:
   an *open* workload, where new users arrive whatever the latency. Check status 200 and that the data is
   there, and assert zero failed requests.
3. **Server metrics.** Add `spring-boot-starter-actuator`, expose the `metrics` endpoint, and configure
   `management.metrics.distribution.percentiles.graphql.request: 0.5, 0.95, 0.99`.
4. **Measure before**: `bin/course shell -c './measure.sh before'` (40 s). It starts the app with JDK Flight
   Recorder and async-profiler attached, warms it up for 10 s, runs Gatling for 20 s, and writes
   `results/before-*`: Gatling's summary, a `.jfr`, a flame graph (open the HTML in a browser) and
   Micrometer's percentiles. Try `jfr view hot-methods results/before.jfr`.
5. Bring in your loaders from lab 9.3, then **measure after**: `./measure.sh after`.

## Write the report

`report.md`: both latency tables (Gatling and Micrometer, before and after), where the time goes (flame
graph, hot methods), and why Gatling's and Micrometer's p99 disagree. It is a checkpoint file.

## Done when

`bin/course test m09/07` is green (it runs JMH and one short `measure.sh` itself, and checks that `app/` is
batched) and `bin/course grade m09/07` passes the report.

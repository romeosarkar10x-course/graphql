# Mini-project 9 — An N+1 detector for JUnit 5

**Lectures:** [9.1](../../lectures/9.1-seeing-n-plus-one.md), [9.3](../../lectures/9.3-java-dataloader-spring.md) · **Grading:** auto · **Needs:** postgres

N+1 comes back. Someone adds a field, writes the obvious resolver, and every test still passes, because
tests check data, not statements. Write the guard: a JUnit 5 extension that fails any test whose body
runs more than its statement budget, and run it in CI.

The starter is the batched app (lab 9.3) with two additions: `SqlCounter` is public and can record the
SQL text of every statement (`startRecording()`, `stopRecording()`, `statements()`), and `Gql.post(port, query)`
in `src/test/java` is a minimal GraphQL client for tests.

## The contract

- `@dev.shelfie.perf.detector.MaxStatements(n)` on a test method, or on a test class for all its
  methods. A method's annotation overrides its class's; a `@Nested` class inherits its outer class's
  budget. Using the annotation is enough to switch the extension on (meta-annotate it with
  `@ExtendWith`).
- Only the test method's body counts: statements run in `@BeforeEach` don't.
- Over budget fails the test with a message that starts `ran <count> SQL statements, budget <n>.` and
  lists the most repeated statements with their counts, like `10x select ... from club c1_0 where c1_0.id=?`.
  A statement repeated once per parent is the N+1.
- A test that fails on its own reports its own failure, not a budget message.
- No annotation, no budget.

## Do

1. Write `MaxStatements` and `StatementBudgetExtension` in `src/test/java/dev/shelfie/perf/detector/`.
   Pick the right extension points: there are callbacks that run around the test method only.
2. Give the app at least three budgets in your own tests (`@SpringBootTest(webEnvironment = RANDOM_PORT)`):
   the dashboard (`ci/dashboard.graphql`) and two more queries. `mvn test` must pass.
3. CI: a GitHub Actions workflow, `.github/workflows/n-plus-one.yml`, that starts PostgreSQL as a service, loads the
   data with `ci/ClubData.java` (it needs the PostgreSQL JDBC driver on the class path), and runs
   `mvn test` against it. It is a checkpoint file.

## How it's tested

`bin/course test m09/11` runs your tests, then copies your project, adds nine fixture tests of its own,
some built to fail, and checks every outcome and failure message.

## Group variant (pairs)

One person writes the extension, the other writes three budgets for the app and one deliberately
naive resolver on a branch. Swap: does the extension catch the other's N+1, and does its message point
at the right statement? Then write the workflow together.

## Done when

`bin/course test m09/11` passes.

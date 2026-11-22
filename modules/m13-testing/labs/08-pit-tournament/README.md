# Game 13.8: Mutation testing tournament

Lecture: [13.1 Unit tests](../../lectures/13.1-unit-tests.md) (and 13.2). Grading: auto. Needs nothing.

PIT (pitest-maven 1.30.2, JUnit 5 plugin 1.2.3, on Boot's JUnit 6) changes the bytecode of Shelfie's services one
bug at a time (a `<` becomes `<=`, a condition is negated, a return becomes null or empty) and runs your tests
against every mutant. A mutant your tests do not notice **survived**. Your score is the share killed: the target
is **80%**.

Start from your lab 13.1 tests (copy `src/test` over) or from scratch. PIT mutates `dev.shelfie.app.service.*`
only; the grader runs your tests against the contract app.

```bash
bin/course start m13/08
cd workspace/m13-testing/08-pit-tournament
cp -r ../01-unit-tests/src/test src/                       # optional head start
bin/course shell -c 'mvn -q test-compile org.pitest:pitest-maven:mutationCoverage'
# open target/pit-reports/index.html: every surviving mutant, line by line
bin/course test m13/08                                     # score, survivors, pass at 80%
```

`bin/course test` writes your score to `target/pit-score.txt` (score, mutants, killed, time).

## Solo

Reach 80%, then push for more. Each surviving mutant is a question: is it a missing test, or an equivalent mutant
(a change no test could ever see)? Note at least two of each kind if you meet them.

## Group variant

Everyone starts from the same starter at the same time, 45 minutes on the clock. Highest score wins; ties go to
the fewer tests (`grep -c @Test`), then to the faster run. At the end, the winner shows the test that killed the
mutant nobody else killed. Second round: swap test suites and each player gets 15 minutes to raise someone
else's score; points for every extra mutant killed.

## Done when

`bin/course test m13/08` passes: your tests pass and kill at least 80% of PIT's mutants.

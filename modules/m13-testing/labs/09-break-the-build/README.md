# Game 13.9: Break the build

Lecture: [13.5 Schema checks in CI](../../lectures/13.5-schema-checks.md). Grading: auto + rubric. Needs nothing.

Twelve rounds. Each round is one sneaky change to Shelfie's schema (`tests/rounds/NN/new.graphqls` against
`tests/old.graphqls`). Call each one **from the diff alone, before CI does**: safe, dangerous or breaking for
existing clients.

```bash
bin/course start m13/09
cd workspace/m13-testing/09-break-the-build
R=/course/modules/m13-testing/labs/09-break-the-build/tests
bin/course shell -c "diff $R/old.graphqls $R/rounds/01/new.graphqls"
# write your call in calls.txt: "01 safe", "02 dangerous", ...
bin/course test m13/09
```

The grader scores your calls against the spec's rules (the answers are hashed in `tests/key.txt`, so reading
the folder does not spoil it): 10 of 12 to pass. Then it prints CI's verdict, graphql-java's `SchemaDiff` as in
lab 13.5. **CI is not always right.** Where your call and CI's differ, `why.md` says who is right and why, with
the spec rule; run `bin/course inspector diff` for graphql-inspector's view as a third opinion.

## Group variant

Each player secretly writes one round: a copy of `tests/old.graphqls` with one change they think the others will
call wrong, plus the true verdict and the spec rule in a sealed note. Rounds go in a shared folder as
`rounds/<name>/new.graphqls`. Everyone calls every other round on paper, then CI runs
(`bin/course java /course/modules/m13-testing/contract/RoundVerdicts.java old.graphqls rounds`). Scoring: one
point per right call; the author gets one point per player fooled, and two more if CI is fooled too (round 06
style). Disputes are settled by the spec section the author quotes.

## Done when

`bin/course test m13/09` passes (10 of 12 calls) and `why.md` passes the rubric (`bin/course grade m13/09`).

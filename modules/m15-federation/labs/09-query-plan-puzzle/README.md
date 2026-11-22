# Game 15.9: Query-plan puzzle

Lecture: [15.5 Composition and query plans](../../lectures/15.5-composition-plans.md). Grading: auto. Needs the
federation tools (`bin/course fed`).

Ten queries over the contract's four subgraphs in `puzzles/`. For each, predict Apollo Router's plan as steps,
each step the set of subgraphs called at the same time, in `answers.json`:

```json
{ "p01": [["catalog"]], "p02": [["reviews"], ["accounts", "catalog"]] }
```

Read only the schemas ([contract/subgraphs/src/main/resources/graphql](../../contract/subgraphs/src/main/resources/graphql/)):
no router, no `plan_steps`, until you have written all ten. 7 exact to pass. The test names wrong answers and
never corrects them.

```bash
bin/course start m15/09
cd workspace/m15-federation/09-query-plan-puzzle
bin/course test m15/09
```

**Solo:** 7 of 10, then aim for 10 with as few runs as possible. **Group:** each player writes
`answers/<name>.json` without talking; `bin/course test m15/09` prints a leaderboard of every file in
`answers/`. Then argue the disagreements before the reveal (`plan_steps`, lecture 15.5).

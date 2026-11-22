# Lab 15.5: Composition and query plans

Lecture: [15.5 Composition and query plans](../../lectures/15.5-composition-plans.md). Grading: auto + rubric.
Needs the federation tools (`bin/course fed`).

The repository layout a federated graph lives in: `subgraphs/` (each team's SDL), `supergraph.yaml`, and
`compose/` (The Guild's MIT composition library, pinned in `package-lock.json`).

**Part 1, composition in CI.**

- `ci/compose.sh [supergraph.yaml] [out.graphql]` composes and writes the supergraph on success. On failure it
  exits non-zero, prints the composition error codes and **leaves the last good supergraph untouched**.
- `.github/workflows/supergraph.yml` runs it on every pull request that touches `subgraphs/`, sets up Node.js,
  and keeps `supergraph.graphql` as an artifact.

The test runs your script on the four subgraphs and on a broken copy, and reads your workflow. It does not run it
on GitHub.

**Part 2, query plans.** For each query in `queries/`, predict the router's subgraph calls **before** you run
anything, as steps (each step the set of subgraphs called at the same time):

```json
{ "q4": [["reviews"], ["accounts", "catalog"]] }
```

Write the predictions in `predictions.json`; the test asks Apollo Router for its plan of each query
(`Apollo-Expose-Query-Plan: true`), reduces it with [contract/PlanSteps.java](../../contract/PlanSteps.java), and
compares. Keep your first guesses for `notes.md`: the lab is in the differences.

```bash
bin/course start m15/05
cd workspace/m15-federation/05-composition-plans
bin/course shell -c 'bash /course/modules/m15-federation/contract/stack.sh'     # in one terminal
bin/course shell -c 'source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh; plan_steps apollo http://localhost:4000/ "$(cat queries/q1.graphql)"'
bin/course test m15/05
```

`n1.graphql` is the cross-subgraph N+1 question: three clubs, every member, every review they wrote, every book.
How many requests does each subgraph get? The test prints the answer after you predict.

## Done when

`bin/course test m15/05` passes and `notes.md` passes the rubric (`bin/course grade m15/05`).

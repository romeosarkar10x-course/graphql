# Mini-project 15.8: Team simulation, club book of the month

Lecture: [15.7 Operating a federated graph](../../lectures/15.7-operating.md). Grading: auto + rubric (RETRO.md).
Needs the federation tools (`bin/course fed`) and, for the checkpoint, Docker Compose on your machine.

The starter is Shelfie's four subgraphs in one Maven project (one jar, one Spring profile per subgraph), a
router config, a script that composes the supergraph from the running subgraphs (`ci/supergraph.sh`), and half of
a `compose.yaml`. Ship **club book of the month**, each team in its own subgraph and schema file:

| Team | Field |
|---|---|
| catalog | `Book.blurb: String!`: "Good Omens by Terry Pratchett and Neil Gaiman (1990)" |
| accounts | `User.displayName: String!`: "Asha Rao (@asha)" |
| reviews | `Book.topReview: Review`: highest rating, the newest (higher review number) on a tie, null without reviews |
| clubs | `Club.bookOfTheMonth: BookOfTheMonth` (`month`, `book`, `pickedBy`), and `pickBookOfTheMonth(clubId, bookId, month): Club`, for the club's owner only (`X-Shelfie-User`), `month` as `YYYY-MM`. Seed: c1 picked b6 in 2026-11 by u11, c3 b9 by u5, c2 nothing |
| platform | whatever the router needs for the mutation to know who is calling |

Then finish `compose.yaml` so that **`docker compose up --wait`** in this directory builds the jar, starts the
four subgraphs, composes the supergraph once they are healthy, and starts the router on `localhost:4000`.

```bash
bin/course start m15/08
cd workspace/m15-federation/08-project-team
bin/course test m15/08
docker compose up --wait && curl -s localhost:4000/ -H 'content-type: application/json' -d '{"query":"{ club(id: \"c1\") { bookOfTheMonth { book { blurb } } } }"}'; docker compose down
```

The test builds your jar, composes from `_service`, runs Apollo Router with your `router.yaml` and checks the
feature (`tests/team.hurl`); it reads `compose.yaml`, and `bin/course checkpoint 15` really runs it.

**Solo:** play every team, one commit per team, in the order you would ship. **Group (four, plus one on the
router):** each person owns one subgraph's files and may not edit the others'; agree on the contract in the
schema descriptions only; merge when composition passes. Either way, write `RETRO.md`: what each team shipped and
in which order, where you had to agree, what composition caught and what it could not.

## Done when

`bin/course test m15/08` passes, `docker compose up --wait` brings the stack up, and `RETRO.md` passes the rubric
(`bin/course grade m15/08`).

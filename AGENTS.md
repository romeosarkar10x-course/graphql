# AGENTS.md

Project: **GraphQL in Java: Zero to Production** — a hands-on course built from the plan in
[plan/syllabus.md](plan/syllabus.md). Root instruction file for any AI agent **building, grading or
tutoring** this course. `CLAUDE.md` points here.

## Read before doing anything

| # | File | What it holds |
|---|---|---|
| 1 | [CONTEXT.md](CONTEXT.md) | Locked decisions and the user's words behind them |
| 2 | [DONE.md](DONE.md) | What exists and is verified |
| 3 | [TODO.md](TODO.md) | Ordered remaining work |
| 4 | [VERSIONS.md](VERSIONS.md) | Pinned versions, verified against Maven Central, and where they differ from the plan |
| 5 | [plan/](plan/) | The approved course plan: syllabus, projects and games, RFC tracker, case studies, port guide |
| 6 | [docs/AUTHORING.md](docs/AUTHORING.md) | Exact file layout every lecture, lab, project and game must follow |

## Rules for the building agent

1. **Maven, never Gradle.** Every Java build is a `pom.xml`. Gradle appears only where an upstream
   project forces it, and the text says so. (CONTEXT.md §2.)
2. **Follow the syllabus.** Lecture numbers, concepts, builds and experiments come from
   `plan/syllabus.md`. Ask before deviating; record any forced deviation in VERSIONS.md or CONTEXT.md.
3. **Concept first, Java second.** Each lab ships a language-neutral contract (`.graphql` files),
   Hurl tests where the lab talks HTTP, and a README describing behaviour, not Java classes.
4. **Every auto-graded lab must pass `bin/course check --starters`:** solution passes, starter fails.
5. **Rubric work gets a `rubric.md`** and is graded by an agent following `docs/AGENT_GRADING.md`.
6. **Every game and group activity ships a solo variant and a group variant.**
7. **Every challenge has a `HINTS.md`** with exactly three escalating hints; hint 3 still is not the answer.
8. **Verify facts.** Versions from Maven Central, API behaviour by running it against the reference
   server, history from primary sources. Probe output goes in `_workbench/`.
9. **Keep artifacts.** Logs, probes and before/after copies go in `_workbench/<date>-<topic>/`. The user
   asked for them to be kept.
10. **Update DONE.md and TODO.md** as you go.

## Rules for a grading or tutoring agent

- Grading: `docs/AGENT_GRADING.md`. Tutoring: `docs/TUTOR.md`. Never paste a lab's `solution/` to the
  learner; hints come from `HINTS.md`, one rung at a time.

## Conventions

- Modules `modules/mNN-slug/`, labs `labs/NN-slug/`, lab id `mNN/NN` (e.g. `m00/04`).
- Inside the course image the reference server is `http://shelfie-ref:8080`, the slow path
  `http://toxiproxy:8666`. From the host: `localhost:8080` and `localhost:8666`.
- The reference server seed (`reference/shelfie-ref/.../Store.java`) is load-bearing: expected outputs
  in labs depend on it. Change both together.

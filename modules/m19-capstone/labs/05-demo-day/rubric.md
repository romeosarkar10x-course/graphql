# Capstone rubric (100 points)

The syllabus's rubric. The grader reads every stage's documents (`capstone.json` lists them), the code, the
checker's output for stages 1 to 4 (`bin/course test m19/01` ... `m19/04`), `docs/DEMO.md`, and the demo:
live (group) or `docs/demo-transcript.md` and the written answers in `docs/QA.md` (solo).

| Area | Points | Full marks look like |
| --- | --- | --- |
| Schema design | 20 | Client-driven, consistent, sensible nullability, evolvable (stage 1's review, and what changed after it) |
| Correctness and tests | 15 | Meaningful coverage, all CI gates, no failing paths in the demo |
| Performance | 15 | No N+1, justified caching (or a justified absence of it), load-test evidence |
| Security | 15 | Tested auth rules, demand control, trusted documents, a self-run attack report |
| Java craft | 10 | Idiomatic modern Java (records, sealed types, virtual threads where they help), clean layering |
| Architecture and ops | 15 | Clear subgraph boundaries, tracing, runbook |
| Communication | 10 | Clear doc and demo, honest trade-offs |

Rules:

- A stage whose checks fail caps the areas it covers at half marks: stage 1 schema design; stage 2 correctness;
  stage 3 performance and security; stage 4 architecture and ops.
- Numbers must be measured: a load-test or cost figure that the grader cannot trace to a run (a log, a script,
  a probe) scores as absent.
- Shorter tracks (README, "Shorter tracks") rescale the areas they skip onto the others.

Pass: 60/100. Output: the agent grading format (`docs/AGENT_GRADING.md`), one row per area.

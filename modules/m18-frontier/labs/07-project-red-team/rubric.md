# Rubric: 18.7 Agent safety lab (FINDINGS.md, attacks/mine.sh)

Pass mark: 12 points. Points:

| Points | For |
| --- | --- |
| 2 each | A seeded weakness found blind (before the first `bin/course test`), with the time, the attack and the fix |
| 1 each | A seeded weakness the course's attacks showed you, fixed |
| 2 each | An attack of your own in `attacks/mine.sh` that is not one of the course's 12, with what it would have done |
| 2 | For each fix, why it is the right layer (guard, approval, routes, config) |
| 2 | What a real deployment adds that no test here covers (authentication of the MCP client, rate limits, monitoring the audit log) |


The seven seeded weaknesses, for the grader, are in `solution/FINDINGS.md`.

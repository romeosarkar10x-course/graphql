# Rubric, lab 13.3 notes (pass: 8 of 12)

| Points | Criterion |
|---|---|
| 3 | For each planted bug, why only the real dependency catches it (SQL a mock never runs, a topic nobody listens to), and what Keycloak adds even without a planted bug. |
| 3 | Measured cost of the containers (start-up, whole run) against the unit tests, and a sensible split of tests. Invented numbers score 0. |
| 3 | One contract for two servers: which rules could not be shared and why, and the behaviour-versus-protocol difference with the audit numbers the grader printed. |
| 3 | What the black-box contract caught and what it cannot see (events, storage), so where each kind of test belongs. |

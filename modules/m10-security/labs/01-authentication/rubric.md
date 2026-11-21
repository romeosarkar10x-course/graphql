# Rubric, lab 10.1 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | The table is filled from real runs: status and the server's words for every token. Invented results score 0. |
| 3 | Explains why authentication belongs at the HTTP layer: a bad token never reaches parsing or resolvers, one policy for every endpoint, no partial data. |
| 2 | A reasoned sessions-vs-JWT choice for Shelfie that names revocation and CSRF. |
| 2 | States what a stolen token allows and for how long, and at least two ways to limit it (lifetime, audience, keeping tokens out of logs). |

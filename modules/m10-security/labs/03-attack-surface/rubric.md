# Rubric, lab 10.3 notes (pass: 8 of 12)

| Points | Criterion |
|---|---|
| 3 | The cost table holds the learner's own `attack.sh` lines for every attack. Invented numbers score 0. |
| 2 | Crash a node: the depth or width that killed the fork, and what the failing and the next request returned. |
| 3 | Correctly separates what graphql-java and Spring stopped by default (fragment cycles, suggestions, good-faith introspection, the token limit, depth over 100, GET mutations) from what they did not. |
| 2 | For each attack that got through, names the control that would stop it (cost limit, list caps, attempt rate limit, parameterized SQL, trusted documents or introspection off). |
| 2 | The CSRF chain: cookie credentials, a CORS-simple content type accepted as JSON, CSRF protection off, and why removing any one breaks it. |

# Rubric, lab 14.3 notes (pass: 8 of 12)

| Points | Criterion |
|---|---|
| 3 | Each of the six paths named with how the starter leaked through it (including the cache and the shared catalogue). |
| 3 | Defence in depth: token as the only source, RLS with FORCE and a non-superuser role, LOCAL settings and connection pools, fail closed. |
| 3 | What RLS does not cover (foreign keys, caches outside the database, logs) and how they closed each. |
| 3 | Visibility per plan: how, what it costs (two schemas to test, client schema caches), and why it is not a security boundary on its own. |

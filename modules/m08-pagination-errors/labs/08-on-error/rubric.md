# Rubric, lab 8.8 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 2 | Q1's table is filled from real runs (data, errors, fetcher counts, cached fields) and explains PROPAGATE's `data: null` by walking the `!`s up from u3's name. |
| 3 | Q2 explains that under NULL a `!` means "not null unless errored" (semantic non-null), and that the client must check errors per path (throw on field error, error-aware cache) or it will show a wrong value. |
| 2 | Q3 names a realistic HALT client and the server-side saving, using the measured fetcher count. |
| 3 | Q4 is grounded in the RFC's real status (RFC 1; PROPAGATE stays the default; names changed) and in the prototype's limits (experimental directive, no request property in Java servers), and gives concrete conditions for production use. |

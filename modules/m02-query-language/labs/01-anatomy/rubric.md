# Rubric, lab 2.1 notes.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Experiment 1 | 2 | Card shows `(@null)`; explains that the server returns only selected fields, so the key is absent and Jackson leaves a reference type null |
| Experiment 2 | 3 | Reports the `MismatchedInputException` (null into `int`); explains primitives cannot be null and that Jackson 3 fails on null for primitives by default; suggests `Integer` or selecting the field |
| Experiment 3 | 2 | No error: Jackson 3 ignores unknown properties by default. Weighs it: tolerant of new fields, but silently hides a typo in a record name |
| The envelope | 3 | `data` (result of execution, may be null or partial), `errors` (only when something went wrong, may sit beside data), `extensions` (optional, implementation-defined: tracing, cost, ...) |

Pass: 7/10.

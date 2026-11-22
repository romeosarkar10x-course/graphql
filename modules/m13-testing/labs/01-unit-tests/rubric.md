# Rubric, lab 13.1 notes (pass: 6 of 9)

| Points | Criterion |
|---|---|
| 3 | Names the tests that caught the deleted resolver logic (from the grader's output) and explains why the service tests could not: the bug sits between the schema and the service. |
| 3 | A reasoned split between unit and slice tests: rules with many cases in unit tests, schema-dependent behaviour (defaults, argument names, context, error mapping) in the slice, with the cost of each. |
| 3 | What mocking cost or saved, with an example from their own tests (brittle mocks of an API, or a real repository used because it is plain data). |

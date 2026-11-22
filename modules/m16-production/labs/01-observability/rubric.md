# Rubric: 16.1 observability (FINDINGS.md)

The code is graded by the tests. This rubric grades the experiment. Pass mark: 6 of 10.

| Points | Criterion |
| --- | --- |
| 3 | The slowest field is found from traces or metrics, with the query (TraceQL or PromQL) that found it and the numbers it showed |
| 2 | Says why total time and single-span time can rank fields differently, or why a batched field does not appear as a field span |
| 3 | Names at least two things the default instrumentation lacked and why each matters on a dashboard (one endpoint, cardinality) |
| 2 | Bonus section: compares the Java agent's spans with the Micrometer ones from an actual run, or says why it was skipped |

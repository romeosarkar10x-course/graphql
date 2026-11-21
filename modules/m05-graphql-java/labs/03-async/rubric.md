# Rubric, lab 5.3 notes.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Measurements | 2 | All six rows, with the 1,000-field platform estimate written before measuring |
| Async is the fetcher | 2 | Explains that the strategy only overlaps futures; blocking fetchers serialise |
| Common pool | 2 | Shared, sized for CPU work (cores - 1), blocking starves unrelated work |
| Limits of virtual threads | 2 | Downstream capacity (connection pools) or pinning: a concrete limit |
| Serial mutations | 2 | Names `AsyncSerialExecutionStrategy` and the spec's serial execution of the mutation root |

Pass: 7/10.

# Rubric, lab 5.8 notes.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Throughput | 2 | Both rows with real numbers and the speed-up |
| Cache key | 2 | Variables are execution inputs; inlined values destroy the hit rate |
| Bounding | 2 | A concrete attack on an unbounded cache (unique documents, memory) |
| Skipped validation | 2 | Schema reload or a shared cache; a fix (per-schema cache, version in key, flush) |
| Cancellation | 2 | Flag checked between steps, running fetchers not interrupted, deadlines downstream |

Pass: 7/10.

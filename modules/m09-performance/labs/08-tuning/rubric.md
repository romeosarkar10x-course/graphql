# Rubric, lab 9.8 report (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 2 | The runs: all four configurations, with load, heap and machine stated, from `tune.sh` output. Invented numbers score 0. |
| 3 | Explains what each change did with evidence beyond the percentiles: GC pause totals, GC log lines (allocation stalls), thread model, what was actually the bottleneck (CPU, pool, heap). |
| 2 | The winner follows from the evidence, and the report separates differences that are bigger than run-to-run noise from those that aren't. |
| 2 | Names conditions that would change the answer (heap size, live data, request rate, slow downstream calls, an SLO at a higher percentile). |
| 1 | Mentions the document cache's effect, or why it is the same for every configuration. |

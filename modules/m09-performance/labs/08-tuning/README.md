# Lab 9.8 — Engine and JVM tuning: four configurations, one winner

**Lecture:** [9.8 Engine and JVM tuning](../../lectures/9.8-engine-jvm-tuning.md) · **Grading:** both · **Needs:** postgres

The starter is the batched app with Micrometer (lab 9.7), the same Gatling load, `measure.sh`, and
`tune.sh`, which runs `measure.sh` once per line of `configs.txt` and writes `results/tuning.md`.

## Do

1. **Document caching.** Lab 9.7's benchmark measured what parsing and validating cost. Add a bounded
   `PreparsedDocumentProvider` (Caffeine) to the app.
2. **Four configurations** in `configs.txt`, one `name: JVM options` per line: platform threads and
   virtual threads (`-Dspring.threads.virtual.enabled=true`), each with G1 (`-XX:+UseG1GC`) and ZGC
   (`-XX:+UseZGC`). Every run gets the same heap (`HEAP`, default 256m) and the same load (`RATE`, default
   150 dashboards/s; `DURATION`, default 20 s).
3. Run them all: `bin/course shell -c './tune.sh'` (about 5 minutes). Look at every row, and at the GC
   logs (`results/<name>-gc.log`) and app logs if a row looks wrong. Then try `HEAP=1g ./tune.sh`.

## Write the report

`tuning-report.md` starts with `Winner: <config name>`, then: the runs (with load, heap and machine),
what each change did and why, why this winner, and what would change the answer. The test re-runs your
winner at a lighter load to check it starts, holds, and uses the collector you claim.

## Done when

`bin/course test m09/08` is green and `bin/course grade m09/08` passes the report.

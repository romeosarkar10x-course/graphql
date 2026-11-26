# Tuning runs (150 dashboards/s for 20 s, -Xmx1g)

| Config | Options | p50 ms | p95 ms | p99 ms | Failed | GC pauses (ms, sum) | Collector |
|---|---|---|---|---|---|---|---|
| platform-g1 | `-XX:+UseG1GC` | 9 | 14 | 26 | 0 | 436 | G1 |
| virtual-g1 | `-XX:+UseG1GC -Dspring.threads.virtual.enabled=true` | 9 | 12 | 15 | 0 | 392 | G1 |
| platform-zgc | `-XX:+UseZGC` | 11 | 15 | 19 | 0 | 3 | The Z Garbage Collector |
| virtual-zgc | `-XX:+UseZGC -Dspring.threads.virtual.enabled=true` | 11 | 15 | 20 | 0 | 4 | The Z Garbage Collector |

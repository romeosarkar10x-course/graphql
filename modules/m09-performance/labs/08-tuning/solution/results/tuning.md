# Tuning runs (150 dashboards/s for 20 s, -Xmx256m)

| Config | Options | p50 ms | p95 ms | p99 ms | Failed | GC pauses (ms, sum) | Collector |
|---|---|---|---|---|---|---|---|
| platform-g1 | `-XX:+UseG1GC` | 10 | 19 | 28 | 0 | 963 | G1 |
| virtual-g1 | `-XX:+UseG1GC -Dspring.threads.virtual.enabled=true` | 10 | 15 | 27 | 0 | 941 | G1 |
| platform-zgc | `-XX:+UseZGC` | 20,001 | 60,000 | 60,001 | 2,984 | 8 | The Z Garbage Collector |
| virtual-zgc | `-XX:+UseZGC -Dspring.threads.virtual.enabled=true` | 20,001 | 45,301 | 60,000 | 2,966 | 22 | The Z Garbage Collector |

# Tuning runs (150 dashboards/s for 20 s, -Xmx1g)

| Config | Options | p50 ms | p95 ms | p99 ms | Failed | GC pauses (ms, sum) | Collector |
|---|---|---|---|---|---|---|---|
| platform-g1 | `-XX:+UseG1GC` | 10 | 15 | 27 | 0 | 466 | G1 |
| virtual-g1 | `-XX:+UseG1GC -Dspring.threads.virtual.enabled=true` | 9 | 13 | 17 | 0 | 395 | G1 |
| platform-zgc | `-XX:+UseZGC` | 12 | 16 | 21 | 0 | 3 | The Z Garbage Collector |
| virtual-zgc | `-XX:+UseZGC -Dspring.threads.virtual.enabled=true` | 11 | 16 | 21 | 0 | 4 | The Z Garbage Collector |

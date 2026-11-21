# Native image race

Reference run, course image + GraalVM CE 25.0.2 (`ghcr.io/graalvm/native-image-community:25`), 24-core
build machine, 2026-11-21 (`_workbench/2026-11-21-m06-native/`, `_workbench/2026-11-21-m05-probe/bake-off.txt`):

| App | Build | Build time | Binary MB | First answer ms (median of 3) | RSS MB | p95 ms |
|---|---|---|---|---|---|---|
| Spring, JVM | `mvn package` | not timed | jar: 27 | 1,109 | 607 | 1.57 |
| Spring, native | `bin/course native -Pnative native:compile -DskipTests` | 43 s | 101 | 49 | 155 | 0.66 |
| Quarkus, JVM | `mvn package` | not timed | jar: fast-jar dir | 519 | 518 | 1.42 |
| Quarkus, native | `bin/course native package -DskipTests -Dquarkus.native.enabled=true` | 33 s | 62 | 32 | 93 | 1.32 |

Quarkus native wins the race (32 ms to the first correct GraphQL answer), Spring native second (49 ms).

## What broke, and how you fixed it

Nothing in the reference apps: no reflection outside what the frameworks register, records and plain
classes only. Spring's AOT step (`process-aot`) generated the reflection hints for the controllers.

## Questions

1. Class loading and initialisation, classpath scanning for components, building the bean graph or CDI
   graph, parsing the schema and wiring it, and JIT warm-up. Native image (with Spring AOT or Quarkus's
   build steps) ran the scanning and much initialisation at build time and snapshotted the heap.
2. Builds take 3 to 4 times longer and need lots of memory; reflection, proxies and resources must be
   declared (hints) or they fail at runtime; there is no JIT, so peak throughput on a long run can be lower
   than a warmed JVM (the free Serial GC and no profile-guided optimisation); and debugging, heap dumps and
   agents (APM) work differently or not at all.
3. No: an always-on Shelfie starts once a week; a second of startup is noise and the JVM's peak
   performance and tooling matter more. It flips for scale-to-zero or per-request deployments (serverless
   functions, Knative, CLI tools) and for dense multi-tenant hosting where 150 MB versus 600 MB of RSS per
   instance is money.

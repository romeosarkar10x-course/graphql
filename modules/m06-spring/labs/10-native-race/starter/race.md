# Native image race

| App | Build | Build time | Binary MB | First answer ms (median of 3) | RSS MB | p95 ms |
|---|---|---|---|---|---|---|
| Spring, JVM | `mvn package` | | jar: | | | |
| Spring, native | `bin/course native -Pnative native:compile -DskipTests` | | | | | |
| Quarkus, JVM | `mvn package` | | jar: | | | |
| Quarkus, native | `bin/course native package -DskipTests -Dquarkus.native.enabled=true` | | | | | |

## What broke, and how you fixed it

## Questions

1. Where did the startup time go? Name two things the JVM version does at startup that the native
   binary did at build time.
2. What did you give up for the faster start? Think about peak throughput, build time, reflection,
   and debugging.
3. For Shelfie, running as an always-on server behind a load balancer, is native worth it? For which
   deployment would your answer flip?

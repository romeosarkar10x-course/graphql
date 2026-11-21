# DESIGN.md — the tiny starter

1. **API.** Show the smallest app someone writes with your library. What did you make convention
   (no annotation needed) and what explicit, and why?
2. **Fail fast.** Which mistakes does `build()` catch at startup, and which still only show up at
   request time? Compare with Spring's schema inspection (lab 6.3).
3. **Scanning.** Why did running from the jar need different code from running from `target/classes`?
   What does Spring use instead of hand-rolled scanning, and what would GraalVM native image (game 6b)
   make of yours?
4. **Reflection cost.** Every request calls `Method.invoke`. Measure or estimate the cost against your
   Module 5 server (lab 6.9's Bench works: `-- java -jar target/tiny-graphql.jar 4000`), and say whether
   it matters.
5. **What next.** Name three features you would add before anyone else used this library, in order.

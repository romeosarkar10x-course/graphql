# The attack surface: notes (reference)

## What each attack cost the server

From `bin/course test m10/03 --solution` on 2026-11-21 (`_workbench/2026-11-21-m10-probe/lab03-solution-run.txt`),
the fork on a 512 MB heap, 24-core machine. Server CPU counts every JVM thread (garbage collector and JIT
included), which is why it can exceed wall time.

| Attack | Status | Time | Response | Server CPU | Memory (RSS) |
|---|---|---|---|---|---|
| depth (98 levels) | 200 | 12 ms | 1,151 B | 50 ms | 491 to 493 MB |
| aliases (500 × `books(first: 60)`) | 200 | 405 ms | 8.1 MB | 3,610 ms | 494 to 615 MB |
| fanout (depth 8, 241,230 field fetches) | 200 | 297 ms | 5.1 MB | 880 ms | 637 to 686 MB |
| cycle | 200, `FragmentCycle` | 8 ms | 348 B | 10 ms | no change |
| suggestions | 200, `FieldUndefined`, no suggestion | 2 ms | 214 B | ~0 | no change |
| introspection | 200 | 5 ms | 6.8 KB | 20 ms | no change |
| injection | 200 | 9 ms | 85 B, the vault row | 50 ms | +1 MB |
| brute force | 6 HTTP requests, 6,000 attempts | | | | |

Depth alone is cheap here: each level has one review, so 98 levels are 98 fetches. Depth hurts when lists
multiply, which is what fan-out does: 8 levels of `first: 10` and `first: 12` lists were 241,230 fetches
from a 300-byte query. Aliases multiply the same way sideways: one 30 KB request cost 3.6 s of CPU.

## Crash a node

Depth 7 (`fanout`) survived. Depth 9 (`_workbench/2026-11-21-m10-probe/attacks/fanout-9.graphql`) killed it:
`Terminating due to java.lang.OutOfMemoryError: Java heap space`, and the next request got no connection
at all. One anonymous request took the server down for everyone. On a 1 GB+ heap (no `-Xmx`, 30 GB
machine) depth 11 instead ran 95 s, used 1,064 s of CPU across all cores, grew to 8.3 GB and answered
500, and the server kept answering 500 afterwards (`_workbench/2026-11-21-m10-probe/attacks.txt`).

## Which attacks graphql-java and Spring stopped on their own

- **Fragment cycles**: refused at validation (`FragmentCycle`), 8 ms.
- **Field suggestions**: graphql-java 26.1 never suggests ("Field 'invitecode' in type 'Club' is undefined"),
  so suggestion-based schema recovery (Clairvoyance) gets nothing from it.
- **Abusive introspection**: the good-faith check refused an introspection query that asked for
  `__Type.fields` twice. A normal introspection query (GraphiQL's) passes.
- **Huge documents**: the parser stops at 15,000 tokens, which capped the brute force at 1,000 attempts a
  request (14 tokens each). It slowed the attack; it did not stop it.
- **Depth over 100**: refused by graphql-java 26's default `QueryComplexityLimits`.
- **Mutations over GET**: 405. So the GET half of CSRF fails.

## Which attacks only a limit you write would stop (lecture 10.4)

Fan-out and alias overloading are under every default (depth 8, a few thousand fields, but 241,230 fetches),
so they need a cost limit that multiplies by list sizes, plus caps on `first`. The brute force needs a rate
limit on attempts (the resolver calls), not on HTTP requests. Injection and introspection exposure are not
demand problems: parameterized SQL, and introspection off or trusted documents (lecture 10.5).

## The CSRF chain: which three choices made it work

1. The token is accepted from a cookie, so the browser attaches it to any site's request.
2. A filter treats `text/plain` as JSON. `text/plain` is a CORS-simple type, so a cross-site form posts it
   with no preflight; Spring alone would answer 415.
3. CSRF protection is off ("it's an API"), which is only safe when credentials never travel by cookie.
Removing any one breaks the chain; lab 10.10 removes all three.

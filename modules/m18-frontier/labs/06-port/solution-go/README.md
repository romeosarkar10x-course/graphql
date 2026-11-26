# The reviews subgraph in Go (reference port)

Go 1.27.2 and [gqlgen](https://gqlgen.com) v0.17.98 (schema first, with its Federation v2 plugin). Built and checked
on 2026-11-24: `bin/course test m18/06` passes with this directory as the workspace (15 signatures, `_service`,
composes with Java's accounts, catalog and clubs, all 12 `port.hurl` checks, with no change to the test).

```bash
cd workspace/m18-frontier/06-port          # with these files in it
bin/course go run github.com/99designs/gqlgen generate     # only after changing graph/schema.graphqls
bin/course go build -trimpath -ldflags="-s -w" -o reviews .
bin/course test m18/06                     # serve.sh starts ./reviews inside the course image
```

`bin/course go` runs the official `golang:1.27.2-bookworm` image (the course image has no Go), with the module
and build caches in `.cache/go`, and builds a static Linux binary (`CGO_ENABLED=0`) that then runs in the course
image.

## What was the same, what was not

- **Schema first, the contract file itself.** `graph/schema.graphqls` is Module 15's `reviews.graphqls`, unchanged;
  gqlgen generates the executor and the `_entities` plumbing from it and leaves one resolver per field to write.
  The signatures matched on the first run, which the TypeScript port (code first) had to work for.
- **Entities**: `FindBookByID`, `FindUserByID`, `FindReviewByID`; returning `nil, nil` gives a `null` entry in
  `_entities`, as the contract wants for unknown ids.
- **Two surprises**, both in gqlgen's HTTP handler, not in GraphQL:
  1. `handler.New` starts with introspection off, and gqlgen's federation then refuses `_service`
     ("federated introspection disabled"). `srv.Use(extension.Introspection{})` turns it on.
  2. Java's `HttpClient` offers an upgrade to HTTP/2 on plain HTTP (`Upgrade: h2c`), and gqlgen's POST transport
     refuses any request with an `Upgrade` header ("transport not supported", HTTP 400): it leaves those to the
     WebSocket transport. curl and Apollo Router do not send it, so the Hurl suite passed and only the course's
     load generator (`Load.java`) failed, with 1,000 errors in 1,000. The server drops an `h2c` upgrade offer.
- **Errors**: `gqlerror.Error` with `Extensions: {"classification": "UNAUTHORIZED"}` and the field's path,
  the same shape as Spring's.

## Numbers

Measured as Module 18's `numbers.sh` measures Java and TypeScript (`_workbench/2026-11-24-finish/go-numbers.sh`):
1,000 requests, 4 at a time, `{ latestReviews(first: 5) { id rating body author { handle } book { id } } }`, after
200 warm-up requests, three runs, one machine.

| | Java (Module 15 jar, `-XX:TieredStopAtLevel=1 -Xmx192m`) | TypeScript (`node dist/server.js`) | Go (static binary) |
| --- | --- | --- | --- |
| Startup to first answer | 986 ms | 170 ms | 3 to 5 ms |
| Memory (RSS after the run) | 173 MB | 141 MB | 16 to 18 MB |
| p50 / p95 / p99 | 0 / 1 / 2 ms | 0 / 1 / 3 ms | 0 / 0 / 1 ms |
| Artifact | jar + JVM | `dist/` + `node_modules` + Node | one 7.7 MB file |

At this size latency is noise for all three; startup and memory are where Go stands apart, which is what matters
for many small subgraphs or scale-to-zero. The Java and TypeScript figures are from 2026-11-23
(`_workbench/2026-11-23-m18-probe/port/numbers.txt`).

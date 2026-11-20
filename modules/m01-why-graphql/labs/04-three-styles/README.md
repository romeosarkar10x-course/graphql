# Lab 1.4 — Three styles, one domain

**Lecture:** [1.4 GraphQL vs REST vs gRPC vs tRPC vs OData](../../lectures/1.4-comparison.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

The reference server exposes one Shelfie dataset three ways. Read the three implementations side by side:

| Style | Code | Try it |
|---|---|---|
| REST | [`ShelfieRestController.java`](../../../../reference/shelfie-ref/src/main/java/dev/shelfie/ref/rest/ShelfieRestController.java) | `curl localhost:8080/api/clubs/c1` |
| gRPC | [`shelfie.proto`](../../../../reference/shelfie-ref/src/main/proto/shelfie.proto), [`ShelfieGrpcService.java`](../../../../reference/shelfie-ref/src/main/java/dev/shelfie/ref/grpc/ShelfieGrpcService.java) | `grpcurl -plaintext shelfie-ref:9090 list` (inside `bin/course shell`) |
| GraphQL | [`schema.graphqls`](../../../../reference/shelfie-ref/src/main/resources/graphql/schema.graphqls), [`ShelfieGraphQlController.java`](../../../../reference/shelfie-ref/src/main/java/dev/shelfie/ref/graphql/ShelfieGraphQlController.java) | http://localhost:8080/graphiql |

Notice the gRPC service has both *resource* RPCs (`GetClub`, `GetUser`: the same waterfall as REST)
and a *screen* RPC (`GetClubPage`) that some backend developer had to write for this one screen.

## Do

1. Finish `calls.sh` so each line prints what it says. Run it with `bin/course shell`, then `bash calls.sh`.
2. Fill in `tradeoff-matrix.md` for all 5 scenarios.

## Done when

`bin/course test m01/04` is green and `bin/course grade m01/04` gives 7/10+.

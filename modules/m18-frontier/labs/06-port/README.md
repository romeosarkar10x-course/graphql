# Lab 18.6: Porting a subgraph to another language

Lecture: [18.6 Porting to another language](../../lectures/18.6-porting.md). Grading: auto + rubric. Needs Node
(the course's), the federation tools (`bin/course fed`) and internet once (npm).

Since Module 1 every lab has shipped a contract that does not mention Java: `.graphql` files, Hurl checks and a
README describing behaviour. This lab cashes that in. Port Shelfie's **reviews** subgraph from Module 15
([reviews.graphqls](../../../m15-federation/contract/subgraphs/src/main/resources/graphql/reviews/reviews.graphqls))
to TypeScript with [GraphQL Yoga](https://the-guild.dev/graphql/yoga-server) 6.0.1 and
[Pothos](https://pothos-graphql.dev/) 4.16.0 (code-first, with its federation plugin). The starter serves one
field and is not federated.

The contract ([contract/port/](../../contract/port/)):

- **The same API.** Introspection of `Query`, `Mutation`, `Review`, `Book` and `User` must give the same 15 field
  signatures as Java's (`signatures.txt`), nullability included.
- **The same data.** `seed.json` (exported from the Java subgraphs): 12 readers' handles, 10 book ids, 15 reviews.
  New reviews are `r16`, `r17`, ... `POST /_reset` puts the seed back.
- **The same behaviour.** `port.hurl`, unchanged: Java's reviews subgraph passes it, and so must yours. It checks
  the subgraph alone (`_service`, `_entities` for `Review`, `Book` and `User`, `addReview` with and without
  `X-Shelfie-User`) and then through Apollo Router with Java's accounts, catalog and clubs.
- **`serve.sh <port>`** builds if needed and serves on that port. The test runs it inside the course image.

```bash
bin/course start m18/06
cd workspace/m18-frontier/06-port
bin/course node sh -c 'npm ci && bash serve.sh 4003'     # by hand
bin/course test m18/06
```

Go instead? The plan allows gqlgen, and the course has a Go reference port (`solution-go/`, gqlgen v0.17.98): the
same test passes for it. The course image has no Go, so build with `bin/course go` (the official Go image, a
static Linux binary), and let `serve.sh` start the binary:

```bash
bin/course go mod init shelfie/reviews && bin/course go get github.com/99designs/gqlgen@v0.17.98
# write gqlgen.yml (with a "federation:" section, version 2) and put the contract in graph/schema.graphqls, then:
bin/course go run github.com/99designs/gqlgen generate
bin/course go build -o reviews . && bin/course test m18/06
```

(gqlgen's own `init` scaffolds a project too; the course's port wrote `gqlgen.yml` by hand and did not try it.)

Then write `PORT.md`: what was the same, what was not, and the numbers (startup time, memory, a latency sample)
beside Java's.

## Done when

`bin/course test m18/06` passes (same signatures, composes, all 12 `port.hurl` checks) and `PORT.md` passes the
rubric (`bin/course grade m18/06`).

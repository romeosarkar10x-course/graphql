# Game 18.8: Polyglot day

Lecture: [18.6 Porting to another language](../../lectures/18.6-porting.md). Grading: auto. Needs the federation
tools (`bin/course fed`), Node and internet once.

One supergraph, as many languages as the class can field. `team.json` says who runs each subgraph and in what
language; the test starts the local ones, reads every subgraph's `_service { sdl }`, composes them with the
course's composer, routes through Apollo Router (forwarding `X-Shelfie-User`), runs five read-only queries that
cross every subgraph (`tests/polyglot.hurl`), and prints the leaderboard: how many languages.

```json
{ "name": "reviews", "language": "TypeScript", "run": "serve.sh", "dir": "workspace/m18-frontier/06-port" }
```

`run` is `contract` (Module 15's Java jar), `serve.sh` (a directory's `serve.sh <port>`, as in lab 18.6) or
`jar:<path>` (`mvn package` in `dir`, then `java -jar <path> <port>`, as in lab 15.10). Ports: accounts 4001,
catalog 4002, reviews 4003, clubs 4004, anything else from 4005.

## Solo

Three languages: Java from the contract, your Kotlin accounts from lab 15.10
(`workspace/m15-federation/10-polyglot-kotlin`, `jar:target/shelfie-accounts-kotlin.jar`) and your TypeScript
reviews from lab 18.6. Point `team.json` at your workspaces and run `bin/course test m18/08`. Stretch: port
catalog or clubs too (same rules as lab 18.6: the contract's SDL, the seed, the Hurl checks) and add a fourth
language.

## Group

Five or more languages, one subgraph each, every team on its own machine: Java, Kotlin, TypeScript, and whatever
the class knows (Go with gqlgen, Python with Strawberry, C# with Hot Chocolate, Rust with async-graphql; see
[plan/language-port-guide.md](../../../../plan/language-port-guide.md)). A fifth or sixth team adds a subgraph
of its own that extends `Book` or `User` with a new field, for example `Book.priceEur` or `User.badge`, keyed
by `id`. Teams publish their port (`COURSE_PUBLISH=<port>`); the referee's `team.json` lists them by `url`:

```json
{ "name": "clubs", "language": "Go", "url": "http://192.168.1.23:4004/graphql" }
```

The referee runs `bin/course test m18/08` on a projector. Composition errors are the best part: read them aloud,
the owning team fixes and redeploys, run again. The leaderboard is the language count when all five queries pass.

## Done when

`bin/course test m18/08` passes: the subgraphs compose, the five queries pass through the router, and at least
three languages are in the graph.

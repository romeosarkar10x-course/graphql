# Projects, experiments and games

The course carries 74 hands-on builds: 16 games, 20 experiments, 24 mini-projects and 14 capstone-grade ideas. Each is done in Java first and ships with a contract and Hurl tests, so it can be redone in any language. Levels: Easy is under 2 hours, Medium 2 to 6 hours, Hard a weekend or more.

## Games

| Game | Module | Level | How it plays |
| --- | --- | --- | --- |
| API scavenger hunt | 0 | Easy | Answer 8 questions about real repos using only the GitHub explorer's docs pane |
| jshell speedrun | 0 | Easy | Fetch a film title from SWAPI in the fewest jshell lines |
| REST or GraphQL? cards | 1 | Easy | 20 scenario cards; argue a style, score for the strongest trade-off |
| GraphQL golf | 2 | Easy | Fetch a target dataset in the fewest characters |
| Predict the response | 2, 3, 8 | Easy | Write the exact JSON before running; points per correct key |
| Fix the broken query | 2 | Easy | 12 validation errors against a timer |
| Nullability bets | 3 | Medium | Bet on nullable vs non-null; failure scenarios settle each bet |
| Schema Jenga | 7 | Medium | Propose a change per turn; bet if it breaks frozen clients; CI decides |
| Hall of shame | 7 | Easy | Vote on the worst real-world schema anti-pattern and present the fix |
| Guess the query count | 9 | Easy | Guess SQL statements for a query; the counter reveals it |
| Performance race | 9 | Medium | Leaderboard for lowest statement count and p95 |
| Capture the flag | 10 | Hard | 12 flags hidden in a vulnerable Shelfie |
| Red team / blue team | 10 | Medium | Swap servers, attack, patch, re-attack |
| Query-plan puzzle | 15 | Medium | Predict a router's subgraph call sequence |
| Incident game day | 16 | Hard | Diagnose 3 injected faults from telemetry alone |
| Mock working group | 17 | Medium | Propose a spec change; peers play champion, skeptic, implementer |

## Experiments (change one thing, measure, explain)

| Experiment | Module | What you measure |
| --- | --- | --- |
| Round-trip latency lab | 1 | Page load vs round trips × latency, REST vs GraphQL, with Toxiproxy |
| String-built query injection | 2 | Why variables matter, by injecting into `String.format` |
| Coercion matrix | 3 | 15 bad inputs as literal vs variable; which path rejects what |
| Nullability blast radius | 3, 7 | How much of a page survives one failing field |
| Serial vs parallel execution | 4 | Thread and timestamp per root field |
| Duplicate-mutation storm | 4 | Duplicates from 50 concurrent virtual threads, before and after idempotency keys |
| Platform vs virtual threads | 5, 6 | Throughput with 10 slow sibling fields |
| Document cache payoff | 5 | Throughput with and without `PreparsedDocumentProvider` |
| Schema build cost | 5 | Classic builder vs `FastBuilder` for 2,000 types |
| Offset vs keyset | 8 | Latency at 1 million rows, with `EXPLAIN ANALYZE` |
| Status codes by media type | 8 | Same failure under `application/json` vs `application/graphql-response+json` |
| N+1 to 1 | 9 | Statements: naive vs DataLoader vs `@BatchMapping` vs jOOQ `MULTISET` |
| GC and executor shoot-out | 9 | p95 and p99 under G1 vs ZGC and 4 executor setups |
| APQ on the wire | 9 | Bytes and CDN hit rate with and without persisted queries |
| Limits tuning | 10 | Attack suite vs real traffic as depth and cost limits change |
| Backpressure flood | 11 | Memory and drop rate at 10,000 events per second |
| Polling vs subscriptions | 11 | Server load for 500 clients |
| `@defer` time to first paint | 11 | First byte and first paint with and without `@defer` |
| Native image race | 6, 16 | Cold start to first response, JVM vs GraalVM, Spring vs Quarkus |
| LLM query validity | 18 | Share of AI-written queries that validate, and their cost |

## Mini-projects (2 to 6 hours each)

| Project | Module | Level | What you build |
| --- | --- | --- | --- |
| GitHub profile CLI | 2 | Easy | A picocli tool that prints a profile card from one query |
| Query analyzer | 2 | Easy | Reports depth, field count and aliases for any document |
| Records-to-SDL generator | 3 | Medium | Your own code-first library from Java records and sealed types |
| Undo everything | 4 | Medium | Every mutation returns an `undoToken`; `undo(token)` via the command pattern |
| 150-line GraphQL server | 5 | Medium | JDK `HttpServer` + graphql-java, spec-compliant |
| Javalin GraphQL starter | 6 | Medium | An annotation-scanning micro framework on your Module 5 server |
| Terminal book browser | 8 | Medium | A Lanterna TUI paging with cursors |
| N+1 detector | 9 | Medium | A JUnit extension failing operations above N SQL statements |
| Cost meter dashboard | 10 | Medium | Per-client cost spend per minute, fed from `extensions` |
| Live trivia game | 11 | Medium | Kahoot-style rooms, questions by subscription, live leaderboard |
| Multiplayer tic-tac-toe | 11 | Easy | Moves as mutations, board as a subscription, spectators |
| Game of Life stream | 11 | Easy | Each generation streamed to subscribers; change rules via mutation |
| Stock-ticker simulator | 11 | Medium | Random-walk prices pushed by subscription, history via `@stream` |
| Normalised cache | 12 | Hard | A Java client cache passing 25 scenarios |
| Shelfie CLI | 12 | Medium | JLine client with autocomplete from introspection |
| Schema-driven mock server | 13 | Medium | A jar that serves realistic fake data for any SDL |
| Schema-diff PR bot | 13 | Medium | A GitHub App in Java that comments breaking changes on PRs |
| Query fuzzer | 13, 17 | Hard | Generates random valid queries from a schema to stress a server |
| Headless CMS in a weekend | 14 | Hard | Content types in a table become GraphQL types at runtime |
| Directive zoo | 14 | Easy | Invent a playful directive (`@pirate`, `@emojify`) and demo it |
| Open Library gateway | 14 | Medium | GraphQL over a third-party REST API with caching and graceful failure |
| Ops dashboard | 16 | Medium | Grafana board: top operations, error codes, slowest fields, cost |
| GraphQL text adventure | 2–11 | Medium | Rooms and items as types, moves as mutations, other players as subscriptions |
| Resolver flame-graph viewer | 5, 16 | Medium | Turns an execution trace into an interactive flame graph |

## Capstone-grade ideas (a weekend to several weeks)

| Idea | Stretches you on |
| --- | --- |
| Live auction house | Bid concurrency, countdown subscriptions, fairness, idempotency |
| Event ticketing with seat map | Seat holds, expiring locks, real-time availability |
| Multiplayer quiz platform | Subscriptions at scale, anti-cheat authorization |
| Online chess with a bot | Game state machine, move validation, spectator streams |
| Collaborative whiteboard | Subscriptions plus CRDT merge on the server |
| Transit tracker from GTFS feeds | Big static datasets plus live vehicle positions |
| IoT weather network | Device mutations from a Raspberry Pi, alerting subscriptions |
| Personal finance tracker | `Money` scalar, multi-currency, privacy, auditing |
| Recipe graph | Ingredient substitution as graph traversal, `@oneOf` lookups |
| Federated library system | 4 Java subgraphs plus 1 Go subgraph behind a router |
| Query-to-SQL compiler | Compile a selection set into one SQL statement (join-monster style) |
| Mini federation gateway in Java | Query planning and `_entities` fan-out on virtual threads |
| IntelliJ query-cost plugin | Inline cost estimates for `.graphql` files in the IDE |
| AI study buddy over MCP | Curated Shelfie tools for an AI assistant with a safety policy |

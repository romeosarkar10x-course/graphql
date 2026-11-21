# Module 11 — Real-time and incremental delivery

About 8 hours, 7 lectures. By the end Shelfie has live meetup chat that scales across instances, a deferred
club page, and you can pick the right real-time tool per feature.

## Before you start

`bin/course ref up` now also starts **Kafka 4.4.0** (one KRaft node, `kafka:9092` inside the course image),
and PostgreSQL runs with `wal_level=logical` for Debezium. Redis is Module 9's.

- `contract/live.graphqls` is Module 11's Shelfie: chat rooms r1 to r3 (`sendMessage`, `typing`,
  `archiveRoom`), `messageSent` and `typing` subscriptions, the `countdown` experiment, and the club page
  whose `recommendations` take 1.5 s. Every lab app serves it on :4000 over HTTP, WebSocket and SSE.
- `contract/Ws.java` is a graphql-transport-ws client that prints every frame with its time
  (`--read-rate 20` makes it a slow client); `contract/Balancer.java` is a round-robin TCP load balancer
  whose control file drops every connection (`echo "drop 1500" > /tmp/balancer-4100`).
- `contract/Load.java` runs 500 polling or subscribed clients and reads the server's CPU from `/proc`;
  `contract/Sql.java` runs SQL against the course database (there is no `psql` in the image).
- `contract/trivia.graphqls`, `questions.json` and `Play.java`; `contract/tictactoe.graphqls` and
  `Ttt.java`: the games' contracts and terminal clients.
- `COURSE_PUBLISH=4000 bin/course shell` publishes port 4000 on your machine, so friends can join group games.

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 11.1 | [Subscriptions, conceptually](lectures/11.1-subscriptions.md) | [m11/01 messageSent, a source stream that breaks](labs/01-subscriptions/README.md) | auto + rubric |
| 11.2 | [Transports](lectures/11.2-transports.md) | [m11/02 WebSocket and SSE, keep-alives, a reconnecting client](labs/02-transports/README.md) | auto + rubric |
| 11.3 | [Implementing subscriptions](lectures/11.3-implementing.md) | [m11/03 Typing indicator, backpressure, cleanup](labs/03-implementing/README.md) | auto + rubric |
| 11.4 | [Scaling fan-out](lectures/11.4-scaling.md) | [m11/04 Three replicas over Redis and Kafka](labs/04-scaling/README.md) | auto + rubric |
| 11.5 | [Alternatives to subscriptions](lectures/11.5-alternatives.md) | [m11/05 Debezium CDC; polling vs subscriptions](labs/05-cdc/README.md) | auto + rubric |
| 11.6 | [@defer and @stream](lectures/11.6-defer-stream.md) | [m11/06 The club page over multipart/mixed](labs/06-defer/README.md) | auto + rubric |
| 11.7 | [Choosing the right tool](lectures/11.7-choosing.md) | [m11/07 Decision matrix and pitch](labs/07-choosing/README.md) | rubric |

Labs chain: 11.1 → 11.2 → 11.3 → 11.4 → game 11.10, and 11.3 → 11.5, 11.1 → 11.6.

## Games and mini-project

| | Solo | Group |
|---|---|---|
| [m11/08 Live trivia](labs/08-project-trivia/README.md) (mini-project) | write the game engine; a scripted three-player game with a spectator grades it | one host, everyone plays from `Play.java`; then the host kills the server |
| [m11/09 Multiplayer tic-tac-toe](labs/09-tic-tac-toe/README.md) | a fair game: refused moves, wins, draws, spectators | a round-robin tournament with watchers hunting for accepted illegal moves |
| [m11/10 Classroom chat stress test](labs/10-chat-stress/README.md) | nine clients, three replicas, two `kill -9`s: every client gets all 48 messages once, in order | everyone's client in one room, the instructor kills instances; points lost for missing and duplicate messages |

## Changes from the plan

- **The load balancer is `contract/Balancer.java`** (TCP, round-robin per connection, with a drop switch),
  not nginx or HAProxy: the replicas run inside one course container, and the same tool simulates network
  drops for lab 11.2.
- **11.5's CDC runs Debezium's embedded engine** in the server (no Kafka Connect cluster), with in-memory
  offsets.
- **11.6 serves graphql-java 26.1's own payload format**, the earlier draft's (`path` and `label` per
  incremental entry, no `pending`/`completed`). RFC 1110's format needs the pending fragments in the initial
  payload, which graphql-java does not report; the lecture and notes teach the difference. `@stream` is
  not implemented in graphql-java 26.1. Spring for GraphQL 2.1 has no incremental transport, so the lab
  writes one (a `RouterFunction` writing `multipart/mixed`).
- **11.6's "first paint"** is measured as time to first byte; there is no browser client in Module 11.
- **The trivia mini-project and tic-tac-toe are graded by scripted games**; the group variants use terminal
  clients.

## Spec references

- [Execution: Subscription](https://spec.graphql.org/September2025/#sec-Subscription): source stream and
  response stream; RFC [#1127](https://rfcs.graphql.org/rfcs/1127/) source-stream errors; RFC
  [#860](https://rfcs.graphql.org/rfcs/860/) no `@skip`/`@include` on the subscription root.
- [RFC #1110 incremental delivery](https://rfcs.graphql.org/rfcs/1110/) and the
  [GraphQL over HTTP incremental delivery RFC](https://github.com/graphql/graphql-over-http/blob/main/rfcs/IncrementalDelivery.md).
- [graphql-transport-ws](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md) and
  [graphql-sse](https://github.com/enisdenjo/graphql-sse/blob/master/PROTOCOL.md).

## Checkpoint

`bin/course checkpoint 11` (see [checkpoint.md](checkpoint.md)): chat on three replicas (labs 11.4 and game
11.10), the deferred club page (11.6), the trivia game playable (11.8) and the decision matrix. Tag:
`checkpoint-11`.

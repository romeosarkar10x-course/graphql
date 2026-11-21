# Alternatives to subscriptions: notes (reference)

## Polling vs subscriptions, measured

`Load.java` against this solution, 500 clients, one container, 2026-11-21
(`_workbench/2026-11-21-m11-probe/lab05-load*.txt`). One run each; the server had already served a run, so
the JIT was warm except where noted.

Busy room, one message a second, 20 s:

| | client requests | delay median / p95 | server CPU |
|---|---|---|---|
| idle (no clients) | 0 | | 0.8 s |
| poll every 2 s | 4,990 (250 a second) | 1,006 / 1,903 ms | 8.8 s |
| subscribed | 0 (500 sockets) | 52 / 94 ms | 7.9 s |

Quiet room, one message every 10 s, 30 s:

| | client requests | delay median / p95 | server CPU |
|---|---|---|---|
| idle | 0 | | 1.0 s |
| poll every 2 s | 7,495 (250 a second) | 977 / 1,887 ms | 12.6 s |
| subscribed (first run) | 0 (500 sockets) | 84 / 90 ms | 8.3 s |
| subscribed (warm repeat) | 0 | 62 / 82 ms | 4.2 s, of which 1.7 s opening the 500 sockets |

Polling costs clients × poll rate whether anything happened or not: 250 requests a second in both rooms,
and 12.6 s of CPU in the quiet room to deliver 3 messages. Its delay is half the interval on average and up
to a full interval. A subscription costs a socket per client, the connection setup, and one execution of
the selection set per subscriber per event: in the busy room that was 8,500 executions, about the same CPU
as polling, at a twentieth of the delay. Subscriptions win clearly when events are rare and latency matters;
polling stays simple, cacheable and stateless when a few seconds of delay are fine.

## What CDC changed

- Any writer's row becomes an event: the batch-job insert reached the subscriber, and an `UPDATE` did not
  (inserts only, `op = c`).
- The row is the event. With "insert, then publish" a crash between the two loses the event, and a
  rollback after a publish announces a message that never existed; Debezium reads only committed rows.
- The cost: a replication slot that holds WAL until it is read (the solution drops it on stop, a real
  service would not), and lag: Debezium's default `poll.interval.ms` of 500 ms showed as a median delay of
  614 ms for subscribers (`lab05-load-poll500ms.txt`); 50 ms brought it to 52 ms.
- With the in-memory offset store, events written while the server is down are skipped on restart
  (`snapshot.mode=no_data`). A file or Kafka offset store would resume from where it stopped.

## Live queries, webhooks and CDC

- **Live queries** (`@live`, in some servers and clients, not in the spec or graphql-java): the client
  sends a query and the server pushes a new result whenever it changes. Good for "this screen's data",
  bad when the result is big or changes constantly.
- **Webhooks**: the server POSTs to a URL a partner registered. Server to server, survives clients being
  offline, needs retries, signatures and idempotency on the receiver's side. Shelfie: notify a partner
  bookshop of new reviews.
- **CDC**: not a client protocol but the source of events inside the backend, which then feeds
  subscriptions, webhooks or caches alike.
- **Subscriptions**: a client with an open connection that wants specific events now: Shelfie's chat,
  typing indicators and live meetup polls.

# Lab 11.2: Transports

Lecture: [11.2 Transports](../../lectures/11.2-transports.md). Grading: auto + rubric. Needs nothing.

The starter is lab 11.1's solution plus `client/Chat.java`, a chat client that subscribes to a room over
WebSocket (graphql-transport-ws) or SSE (graphql-sse, distinct connections mode) and prints each message
as `<id> <author>: <body>`. It does not survive a dropped connection: it stops, and whatever was sent
meanwhile is lost. Two jobs:

1. **Keep-alives.** An idle subscription hears nothing for minutes, and proxies close silent connections.
   Make the server send a keep-alive within 5 s of silence on both transports (configuration only).
2. **Reconnect and fill the gap.** When the connection drops, reconnect with exponential backoff and
   jitter, then fetch what you missed with `messages(roomId, after: <last id you printed>)`. Print every
   message exactly once, in order, on both transports.

The test runs your client through `contract/Balancer.java`, a TCP proxy that can drop every connection and
refuse new ones for a while, then sends three messages before, three during and three after a 1.5 s drop.

```bash
bin/course start m11/02
cd workspace/m11-realtime/02-transports
bin/course shell -c "mvn -q package && (java -jar target/shelfie-live.jar > /tmp/app.log &) && sleep 8 \
  && (java /course/modules/m11-realtime/contract/Balancer.java 4100 4000 &) && sleep 1 \
  && java -cp \"\$(ls /course/.cache/course-lib/*.jar | grep -v console | paste -sd:)\" client/Chat.java \
     http://localhost:4100/graphql r1 --transport sse --seconds 60"
# from another shell in the same container: echo "drop 1500" > /tmp/balancer-4100
```

In `notes.md`: the same subscription's frames on both transports, side by side; what each transport did
when the connection dropped and what your client logged; and where RSocket and multipart HTTP
subscriptions would fit.

## Done when

`bin/course test m11/02` passes (two keep-alives; nine messages exactly once and in order across a drop,
on each transport) and `notes.md` passes the rubric (`bin/course grade m11/02`).

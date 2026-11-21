# Hints: m11/02

1. Keep-alives are two Spring Boot properties, one under `spring.graphql.websocket`, one under
   `spring.graphql.http.sse`. For reconnecting, the client needs a loop around "connect, wait until it
   drops", and a memory of the last message it printed.
2. Order matters: subscribe first, then query the gap. Messages that arrive live while the gap query is in
   flight must wait, and anything the two overlap on must be printed once: compare message ids (`m12` > `m11`).
3. Hold live messages in a list while a `filling` flag is set, and set it *before* you resubscribe: the new
   subscription can deliver before you start the gap query. After the gap query, print the gap, then the
   held messages, skipping any id at or below the last one printed. Wait for "connected *or* dropped", not
   for "connected" with a long timeout, or each refused attempt costs you seconds.

# Rubric, lab 11.4 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | What the broken relays did in a real run (who got how many of the nine, and why the Kafka numbers came out that way), and what fixed each. Invented results score 0. |
| 3 | Delivery guarantees: Redis pub/sub is at most once and keeps nothing; Kafka keeps the log, so an instance that was down can resume from its offset. What each means for live subscribers and for a history the client can catch up from. Ordering per room (the key). |
| 2 | Sticky sessions: why a WebSocket or SSE connection is already "sticky" and what the balancer must still do (round-robin per connection, health checks, draining on deploy). |
| 2 | What `Room.watching` and the message ids now mean across three instances, and what would have to change for them to be cluster-wide. |

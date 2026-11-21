# Scaling fan-out: notes (reference)

## The broken relays

From `bin/course test m11/04 --starter` (2026-11-21), three instances, nine subscribers, nine messages:

- **Redis, nothing published:** all 9 subscribers missed messages; each got the 3 sent to its own
  instance. Without a shared channel each JVM's sink is an island.
- **Kafka, one consumer group for all instances:** 6 subscribers got none, 3 got all 9. Kafka gives each
  partition to one consumer per group; every r2 event has the key `r2`, so all of them are in one
  partition, read by one instance, delivered only to that instance's 3 subscribers. A consumer group
  shares work (a job queue); fan-out needs every instance to read everything, so each instance gets its
  own group: `groupId = "shelfie-live-${shelfie.instance}"`.

After the fixes, all 9 subscribers got all 9 messages on both buses.

## Delivery guarantees

- **Redis pub/sub is at most once.** Redis forwards a `PUBLISH` to whoever is subscribed at that moment
  and keeps nothing. An instance that is restarting misses those events for good. For live subscribers
  that is acceptable only because they also reconnect and fill their gap from a query (lab 11.2), and the
  query must read a store all instances share.
- **Kafka keeps the log.** An instance whose group has committed offsets resumes where it stopped and
  replays what it missed (the test tags each run's messages because of this). With no subscribers yet
  the replayed events go nowhere, but a cache or a history built from the topic would be complete. A new
  group with `auto-offset-reset: latest` starts at the newest event.
- **Ordering:** Kafka keeps order within a partition, and the room id key puts one room's events in one
  partition, so one room's messages arrive in order everywhere. Redis delivers in publish order over one
  connection.
- Neither gives exactly once to a subscriber: a client that drops mid-event can see it again after a
  reconnect, which is why lab 11.2's client removes duplicates by id.

## Sticky sessions

A WebSocket or an SSE response is one long TCP connection, so it stays on the instance it was balanced to
(`Balancer.java` picks per connection, round-robin): it is sticky by construction. Sticky sessions matter
for things that span several connections: SockJS-style fallbacks, or graphql-sse's single connection mode
where the reservation and the stream are separate requests. The balancer still has to spread new
connections, stop sending them to an instance that is down, and drain an instance before a deploy so its
clients reconnect elsewhere gradually instead of all at once.

## What is still per instance

`Room.watching` counts only this instance's subscriptions (the test asks each instance and gets 3, 3, 3).
Message ids come from each instance's own counter, so `m7` on a and `m7` on b are different messages,
and each instance stores only the messages it accepted: `messages(after:)` gives a different answer on
each. Cluster-wide, both need shared state: ids from one sequence (Redis `INCR`, a database sequence) and
a shared history (Redis, PostgreSQL or the Kafka topic itself), and a count kept in Redis with expiry.

# Checkpoint 11 quiz

Answer without notes, then check.

1. In `subscription { messageSent(roomId: "r1") { body } }`, what is the source stream and what is the
   response stream?
2. A client sees `error` with "Subscription error" and nothing else. What happened, and how do you make
   the server say more?
3. A reconnecting client prints a live message before it queries the gap. What can go wrong?
4. Why is `sample(250ms)` the wrong way to thin a typing indicator?
5. Three instances read one Kafka topic in one consumer group. What do subscribers see, and why?
6. Polling every 2 s or subscriptions, for 500 readers of a room with 3 messages in 30 s: which costs the
   server more, and why?
7. With graphql-java's incremental flag on, what did Spring for GraphQL 2.1 send a JSON client that used
   `@defer`?
8. Name a Shelfie feature you would not build with a subscription, and what you would use.

<details><summary>Answers</summary>

1. The source stream is the room's message events (the `Flux` the root field returns); the response stream
   is the result of executing `{ body }` against each event.
2. The source stream failed; Spring's default resolver hides the cause. A `SubscriptionExceptionResolver`
   maps the exception to a meaningful error (lab 11.1: `SOURCE_STREAM_FAILED`, `retryable`).
3. It moves "the last id seen" past messages the gap query would have returned, so they are skipped (game
   11.10 found `m36` lost this way). Hold live messages from the moment you resubscribe.
4. It keeps one event per window, so with several typists all but one vanish; window per author instead.
5. Each partition goes to one member of a group, and one room's events share a key and a partition: only
   one instance gets them, so two thirds of the subscribers see nothing.
6. Polling: 250 requests a second whatever happens (12.6 s of CPU in lab 11.5) against sockets that sit
   idle between three events.
7. The initial payload with `hasNext: true`; the deferred data was silently dropped.
8. For example the unread badge (polling at 60 s), a partner notification (a webhook fed by CDC), the
   recommendations panel (`@defer`), the admin counter (polling).

</details>

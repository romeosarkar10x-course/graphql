# Hints: m11/04

1. Redis: every instance must both publish to one channel and subscribe to it, and must deliver an event
   only when it comes back from Redis (not also locally, or this instance's subscribers get it twice).
   Kafka: read what a consumer group is for.
2. `ReactiveRedisMessageListenerContainer.receiveLater(new ChannelTopic(...))` gives a `Mono<Flux<...>>`
   that completes once the subscription is in place: block on it at startup, then subscribe to the flux.
   Kafka gives each partition of a topic to one consumer *per group*, and every event for room r2 has the
   key r2, so it lives in one partition.
3. Give each instance its own group id: the listener's `groupId` can use a property placeholder such as
   `${shelfie.instance}`. For Redis, publish with `convertAndSend(channel, json.writeValueAsString(e))`
   and in the listener `json.readValue(message.getMessage(), Event.class)` then `bus.deliver(...)`.

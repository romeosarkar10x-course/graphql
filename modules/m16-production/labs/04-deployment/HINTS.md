# Hints: 16.4 deployment

1. Start with the probes. Spring Boot has a property that turns on the `liveness` and `readiness` health groups
   outside Kubernetes. Then run the deploy once: the dropped count tells you resume is missing. For resume, the
   reviews service can replay the past (`ReviewsClient.events(after, 0)`), and the replica's live feed is
   `sink.asFlux()`.
2. Fetching what was missed, then subscribing to the live feed, leaves a gap between the two. Subscribing
   first, then fetching, gives you everything twice. Start listening before you fetch, buffer what arrives, and
   use `seq` to drop what you already sent. Clean the buffer up when the subscriber goes away.
3. Failed queries mean traffic reached a replica that had stopped listening. Before the web server stops, make
   readiness `REFUSING_TRAFFIC` (`AvailabilityChangeEvent`), then wait longer than the balancer takes to notice.
   Only then complete the subscriptions. A `SmartLifecycle` with a high phase stops before the web server does.
   Kept-alive connections bypass the balancer: what response header makes a client stop reusing one?

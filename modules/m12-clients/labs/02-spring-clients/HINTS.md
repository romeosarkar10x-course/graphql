# Hints: m12/02

1. Make three beans: `HttpSyncGraphQlClient.builder(RestClient.create(url))`, `HttpGraphQlClient.builder(WebClient.create(url))`
   and `WebSocketGraphQlClient.builder(url, new ReactorNettyWebSocketClient())`. Documents in
   `src/main/resources/graphql-documents/` load by name with `documentName("clubDigest")`.
2. `retrieveSync("club").toEntity(ClubDigest.class)` returns null for a null club; the reactive `retrieve`
   gives an empty `Mono` (`defaultIfEmpty`). For the user header, `client.mutate().header(...).build()` gives a
   per-request copy that keeps the interceptors. If the interceptor only ever sees `null` names, look at what
   `documentName` sends and what it does not.
3. Start the subscription once the app is ready (`@EventListener(ApplicationReadyEvent.class)`) with
   `retrieveSubscription("messageSent").toEntity(Message.class)`, and keep it alive across restarts with
   `.retryWhen(Retry.backoff(Long.MAX_VALUE, ...))` before `.subscribe(...)`.

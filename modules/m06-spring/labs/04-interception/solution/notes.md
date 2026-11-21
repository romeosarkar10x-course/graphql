# Lab 6.4 notes

1. HTTP header `X-Shelfie-Tenant: eu` → Spring MVC request → `WebGraphQlRequest` → `TenantInterceptor`,
   which calls `configureExecutionInput` to add `tenant` to the `ExecutionInput`'s `GraphQLContext` →
   graphql-java execution → every `DataFetchingEnvironment` of that request → Spring resolves
   `@ContextValue String tenant` from it for `Meetup.startsAtLocal`, three levels down. Nothing between
   the root and the meetup had to pass it along.
2. Request: the authenticated principal and its scopes, a correlation id for logs, a locale, a
   DataLoader registry per request (Module 8). Response: rate-limit or cost headers (Module 10), a
   `Cache-Control` header computed from what was queried (Module 9), timing extensions for internal
   callers only.
3. When the rule should be declared in the schema and applied uniformly to many fields, possibly by
   people who do not write controllers: formatting, masking (`@redacted`), authorisation (`@auth(role:)`),
   or deprecation tracking. The directive documents the behaviour in the contract and keeps it out of
   every controller; for one field, `toUpperCase()` in the controller is simpler.
4. Spring's controller fetchers return a Reactor `Mono` (probe: `MonoOnErrorResume`), which Spring
   adapts later, so a wrapper sees a `Mono`, never the `String`. Lesson: inside Spring, values from
   fetchers can be `Mono`, `Flux`, `CompletableFuture` or plain, and graphql-java-level code (wrappers,
   instrumentation) must handle all of them, or be written with Spring's own extension points.

# Hints, lab 10.7

1. `AuthenticationWebSocketInterceptor(new BearerTokenAuthenticationExtractor(), authenticationManager)` reads
   `Authorization` from the `connection_init` payload. The manager is a `ProviderManager` around a
   `JwtAuthenticationProvider` built from the app's `JwtDecoder`; give it the same converter as HTTP
   (principal claim, roles).
2. Start the starter with `--logging.level.org.springframework.web.socket=DEBUG` and open a socket from an
   origin with no CORS config at all: Spring logs why it refuses. Then ask where Boot gets the allowed
   origins for the GraphQL WebSocket handler.
3. `spring.graphql.cors.*` configures CORS for `/graphql`. For errors, delete the custom resolver:
   Spring for GraphQL already answers an unresolved exception with `INTERNAL_ERROR` and logs it.

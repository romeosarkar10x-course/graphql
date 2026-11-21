# Hints, lab 10.1

1. Spring Boot configures the whole resource server from properties under
   `spring.security.oauth2.resourceserver.jwt`: `issuer-uri`, `audiences`, `principal-claim-name`,
   `authorities-claim-name`, `authority-prefix`. The `SecurityFilterChain` then needs only
   `.oauth2ResourceServer(o -> o.jwt(...))`.
2. `permitAll()` does not mean "ignore tokens": the bearer filter still runs and rejects a bad token with
   401 before the request is authorized. Disable CSRF and sessions for a bearer-only API, and say why in
   your notes.
3. In a controller, `@AuthenticationPrincipal Jwt jwt` is null for anonymous requests. Outside a
   controller, `SecurityContextHolder.getContext().getAuthentication()` is a `JwtAuthenticationToken`
   whose `getName()` comes from the principal claim you configured.

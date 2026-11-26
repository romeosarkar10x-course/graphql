# Hints: m13/03

1. `@ServiceConnection` on a static `@Container` field wires a PostgreSQL or Kafka container into Spring Boot with
   no properties at all. Keycloak has no service connection: put its issuer
   (`getAuthServerUrl() + "/realms/shelfie"`) into `spring.security.oauth2.resourceserver.jwt.issuer-uri` with
   `@DynamicPropertySource`. Get a real token with the password grant (client `shelfie-cli`, user `ada`, password
   `ada-pw`).
2. If a signed request still comes back `UNAUTHORIZED`, ask whether it went through Spring Security's filter
   chain at all. Which MockMvc does your tester use, and who registered the filters in it? A test on a real port
   (`webEnvironment = RANDOM_PORT`, a `WebTestClient` bound to it) sends requests exactly as a client would.
3. For Kafka, subscribe a plain `KafkaConsumer` (earliest offset, a fresh group id) before the test creates the
   review, then poll with a deadline and look for the book's key. For the contract, keep to behaviour both
   servers share: Module 5 does not trim bodies or refuse a second review, so neither can be in it.

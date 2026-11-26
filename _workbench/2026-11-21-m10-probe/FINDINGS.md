# Module 10 probe findings (2026-11-21)

Keycloak 26.8.2, Spring Boot 4.2.0, Spring Security 7.2.0, Spring for GraphQL 2.1.0, graphql-java 26.1,
Bucket4j 8.22.0, graphql-cop 1.15, Clairvoyance 2.5.5. Raw output in this folder.

## Authentication (lab 10.1)

1. **Keycloak's `kid` is base64url(SHA-256(DER public key))**, not the RFC 7638 thumbprint. Computed both
   for the course key; only the DER digest matched `fPw-NL_CmaX4CjJ28Iw3BB7yUsNVcKeQ7CUvnDFpk-A`. With a
   key provider in the realm import, Keycloak creates no other keys (the JWKS has one key).
   (`keycloak-token.txt`)
2. **Every bad token is a 401 before GraphQL**, with `permitAll()`: expired, forged, `alg: none`
   ("Unsupported algorithm of none"), wrong `aud`, wrong `iss`, malformed. A `Basic` header is ignored and
   the request is anonymous. (`auth-probe.txt`)
3. **Spring Security 7.2 adds `resource_metadata=".../.well-known/oauth-protected-resource"`** (RFC 9728) to
   the `WWW-Authenticate` challenge.
4. **`public-key-location` plus `issuer-uri` verifies offline and still checks `iss`**: no request to
   Keycloak at all (0 log lines), wrong issuer 401. Labs 10.2 on need no Keycloak. (`offline-probe.txt`)
   **Corrected 2026-11-24: wrong on Spring Boot 4.2.0.** With Keycloak stopped, lab 10.2's solution answered 500
   (`JwtDecoderInitializationException ... Unable to resolve the Configuration with the provided Issuer`): Boot's
   `JwtDecoderConfiguration` builds the issuer-discovery decoder when `issuer-uri` is set. The 2026-11-21 probe ran
   with Keycloak up, so the fetch succeeded and left no log line. Every lab app that verifies offline (10.2 to
   10.8, 14.3, 14.6, solutions and starters) now drops `issuer-uri` and declares a `JwtIssuerValidator` bean:
   200 for a good token, 401 for a wrong issuer, expired or wrong audience, with Keycloak stopped
   (`_workbench/2026-11-24-finish/`).
5. **Spring Security adds `Cache-Control: no-cache, no-store, max-age=0, must-revalidate`** to every
   response, GraphQL included. Module 9's finding "Spring sends no Cache-Control" holds only without Spring
   Security on the class path.

## Authorization (lab 10.2)

6. `AccessDeniedException` from `@PreAuthorize` and from a directive wrapper becomes `FORBIDDEN` for a
   signed-in caller; `AuthenticationCredentialsNotFoundException` becomes `UNAUTHORIZED`. No custom resolver.

## The attack surface (labs 10.3, 10.9)

7. **graphql-java 26.1 never suggests field names** ("Field 'invitecode' in type 'Club' is undefined").
8. **Good-faith introspection is on by default**: an introspection query asking for `__Type.fields` twice
   was refused ("not asking for introspection in good faith"). GraphiQL's query passes.
9. **Default limits**: depth 100 (`Query depth 101 exceeds maximum allowed depth 100`), 100,000 fields, and
   the parser stops at 15,000 tokens or 1,048,576 characters. A 1,000-alias `joinClub` brute force is
   14,000 tokens and fits; 10,000 aliases do not. (`limits.txt`)
10. **Fragment cycles** are refused at validation (`FragmentCycle`), 1 ms.
11. **Fan-out is the real cost**: depth 8 with lists of 10 and 12 was 241,230 field fetches from a 300-byte
    query. Depth 9 killed the fork on a 512 MB heap (`OutOfMemoryError`, process exit). On an unlimited
    heap (30 GB machine), depth 11 ran 95 s, used 1,064 s of CPU and 8.3 GB, and answered 500, and the server
    kept answering 500. (`attacks*.txt`, `crash.txt`)
12. **Wall time is a poor flag trigger**: the same query took 0.2 to 0.6 s between runs; the CTF's
    "expensive query" flag counts field fetches (an instrumentation) instead.
13. **Spring for GraphQL refuses mutations over GET with 405** even with GET enabled; `text/plain` is 415
    unless a filter rewrites it. The fork's rewrite plus a cookie token made CSRF work. (`fork-probe.txt`)
14. **A request with only an APQ hash reaches the `PreparsedDocumentProvider` with the query text
    `PersistedQueryMarker`** (`PersistedQuerySupport.PERSISTED_QUERY_MARKER`), not an empty string.

## Demand control (lab 10.4)

15. `QueryComplexityLimits` per request through the `GraphQLContext` key works; messages are
    `Query depth 11 exceeds maximum allowed depth 10` and `Query has 501 fields which exceeds maximum
    allowed 500`; the response has no `data` key at all. `MaxQueryComplexityInstrumentation` reports
    `maximum query complexity exceeded 43388772 > 5000` (also through fragments).
16. **`ExecutionInput.cancel()` from a timer stops a 3 s query at about 1 s** with an error containing
    "cancel".
17. **A zero-cost query broke the budget**: `{ __typename }` costs 0 and Bucket4j refuses to consume 0
    tokens (`IllegalArgumentException`), so the server answered 500. Found by graphql-cop's first request;
    fixed in every lab with `Math.max(cost, 1)`.

## Transport (lab 10.7)

18. **Spring refuses cross-origin WebSocket handshakes by default** ("Rejecting cross-origin WebSocket
    request"), and **Boot gives `spring.graphql.cors` to the GraphQL WebSocket handler**: with no CORS
    config only same-origin sockets open; `allowed-origins: https://shelfie.test` lets only that origin in;
    `allowed-origin-patterns: "*"` lets every origin in. (`ws-origin-probe.txt`)
19. **`AuthenticationWebSocketInterceptor`** (Spring for GraphQL 2.1) authenticates the `connection_init`
    payload's `Authorization`; a forged or expired token closes the socket with `4401 Unauthorized`.
    (`ws-close-codes.txt`)
20. With the default exception handling, an unresolved exception reaches the client as `INTERNAL_ERROR`
    and the detail is in the server log.

## Scanners (lab 10.8)

21. **graphql-cop 1.15 (13 checks)**: 4 HIGH on lab 10.7 plus 10.4's limits (alias overloading at 101
    aliases, field duplication at exactly 500 fields, directive overloading at 10 errors, introspection),
    none after the fixes. On the fork it also reported GET queries, and missed the `text/plain` CSRF (it
    tries url-encoded only), `alg: none`, SQL injection and every authorization bug. (`graphql-cop-*.json`,
    `before.json`, `after.json`)
22. **Clairvoyance 2.5.5 recovers nothing from graphql-java**: no suggestions, and it does not understand
    graphql-java's messages. But those messages sort a wordlist by hand: `SubselectionRequired for type
    '[Book!]!' of field 'books'`, `Missing field argument 'id'`. (`clairvoyance.txt`)

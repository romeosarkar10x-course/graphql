# Lab 6.1 notes

## Load test (`java Load.java <n>` against `slowHello`, 100 ms each)

Course image, 2026-11-21 (`_workbench/2026-11-21-m05-probe/boot-virtual-threads-load.txt`):

| Threads | n = 200 | n = 1000 | n = 2000 |
|---|---|---|---|
| platform (`spring.threads.virtual.enabled=false`) | 125 ms, 1,598 req/s | 519 ms, 1,928 req/s | 1,059 ms, 1,888 req/s |
| virtual | 124 ms, 1,610 req/s | 169 ms, 5,907 req/s | 253 ms, 7,904 req/s |

## Questions

1. 200 threads / 0.1 s = 2,000 requests per second at most: `server.tomcat.threads.max` defaults to 200,
   and each request holds its thread for the whole 100 ms sleep. 1,000 requests need five waves (~500 ms).
   With virtual threads the sleep parks a virtual thread instead, so the cap disappears until something
   else (CPU, connections, the client) limits it.
2. Spring parses the HTTP request and picks the media type, builds the `GraphQL` object from every
   `*.graphqls` under `classpath:graphql/`, maps controller methods to coordinates and converts arguments
   into records. My server did the September 2026 status codes: the audit passes 11 of 25 against Spring
   for GraphQL 2.1 (`_workbench/2026-11-21-m05-probe/spring-graphql-audit.txt`), which answers validation
   errors with 200, has no GET and no 294.
3. Spring MVC (`spring-boot-starter-webmvc`), servlet-based, one thread per request, which virtual
   threads make cheap. WebFlux fits when the whole stack is reactive (R2DBC, WebClient) or when you
   stream (subscriptions over SSE or WebSocket, Module 11) and want backpressure end to end.
4. Spring builds them: `GraphQlSource` holds the schema and the `GraphQL` object (customisable with
   `GraphQlSourceBuilderCustomizer` and `RuntimeWiringConfigurer`, 6.4), the annotated controllers become
   the data fetchers, and the transport builds the `ExecutionInput` from the HTTP request, which
   `WebGraphQlInterceptor` can change (the provided `ViewerInterceptor` does).

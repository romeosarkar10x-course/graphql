# Lab 12.2: Spring's GraphQL clients

Lecture: [12.2 Spring's GraphQL clients](../../lectures/12.2-spring-clients.md). Grading: auto + rubric.
Needs the reference server.

Build **shelfie-digest**, a second Spring Boot service (:4000) that serves REST and is a GraphQL *client*:
of Shelfie (`digest.shelfie-graphql`, default `http://shelfie-ref:8080/graphql`) and of the chat server
(`digest.chat`, default `ws://localhost:4100/graphql`, Module 11's chat in `contract/chat-server/`).

| Endpoint | Does |
|---|---|
| `GET /clubs/{id}` | the club digest through `HttpSyncGraphQlClient`: `{id, name, memberCount, currentBook {title, authors}, owner {name, handle}}`; 404 for an unknown club |
| `GET /clubs/{id}/reactive` | the same through `HttpGraphQlClient`, as a `Mono` |
| `POST /books/{id}/reviews` | `createReview` as the user in the caller's `X-User` header (Shelfie reads `X-Shelfie-User`); 201 with the payload, or 422 with its `userErrors` |
| `GET /chat` | the last 20 messages of room `digest.room` (r1), oldest first, from a `WebSocketGraphQlClient` subscription that starts with the service and survives a chat-server restart |
| `GET /stats` | requests per operation name, counted by a client interceptor on both HTTP clients: `{"clubDigest": {"requests": 2, "withErrors": 0}}` |

Documents live in `src/main/resources/graphql-documents/` (`clubDigest.graphql`, `createReview.graphql`),
not in Java strings. Decode with `retrieveSync(...).toEntity(...)` into the records in `Model.java`.

```bash
bin/course start m12/02
cd workspace/m12-clients/02-spring-clients
bin/course shell -c "cd /course/modules/m12-clients/contract/chat-server && mvn -q package -DskipTests \
  && (java -jar target/shelfie-live.jar --server.port=4100 &) && cd /course/workspace/m12-clients/02-spring-clients \
  && mvn -q package && java -jar target/shelfie-digest.jar"
```

Experiment: call Shelfie from the digest with a broken document and see what `toEntity` throws; stop the
chat server for a few seconds and watch your subscription come back.

In `notes.md`: sync vs reactive for this service, what `toEntity` did with a null field, an invalid field and
userErrors, and what the subscription missed across the restart.

## Done when

`bin/course test m12/02` passes (both clients, 404s, a per-request user, the chat feed across a
restart, the interceptor's counts) and `notes.md` passes the rubric (`bin/course grade m12/02`).

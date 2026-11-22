# Lab 12.4: MicroProfile GraphQL clients

Lecture: [12.4 MicroProfile clients](../../lectures/12.4-microprofile-clients.md). Grading: auto + rubric.
Needs the reference server.

A Quarkus command-line app (`quarkus-smallrye-graphql-client`) prints a club card two ways:

```bash
java -jar target/quarkus-app/quarkus-run.jar typesafe c1
java -jar target/quarkus-app/quarkus-run.jar dynamic c1
```

```
The Midnight Readers (8 members), run by Kemi Adeyemi
Reading: The Left Hand of Darkness by Ursula K. Le Guin (1969)
  4/5 Hugo Silva: Estraven deserved better.
  3/5 Jonas Berg: Beautiful but I wanted more physics.
```

- **typesafe:** a `@GraphQLClientApi` interface whose return type is `Model.Club`.
- **dynamic:** a `DynamicGraphQLClient` with the document built by its builder API.

Both read `quarkus.smallrye-graphql-client.shelfie.url`. Each sends **one** request, passes the club id as
a variable, and asks Shelfie for `reviews(last: 2)` (no trimming in Java). An unknown club prints
`No club <id>`.

```bash
bin/course start m12/04
cd workspace/m12-clients/04-microprofile
bin/course shell -c "mvn -q package && java -jar target/quarkus-app/quarkus-run.jar typesafe c1"
# see what went over the wire: contract/Capture.java prints every request body and forwards it to Shelfie
bin/course shell -c "(java /course/modules/m12-clients/contract/Capture.java 4999 http://shelfie-ref:8080/graphql &) \
  && sleep 2 && java -Dquarkus.smallrye-graphql-client.shelfie.url=http://localhost:4999/graphql \
     -jar target/quarkus-app/quarkus-run.jar typesafe c1"
```

In `notes.md`: both versions side by side and which reads better, what each sent, and where the selection
set's source of truth lives in labs 12.2, 12.3 and 12.4.

## Done when

`bin/course test m12/04` passes (both modes, both clubs, unknown club, the captured requests) and `notes.md`
passes the rubric (`bin/course grade m12/04`).

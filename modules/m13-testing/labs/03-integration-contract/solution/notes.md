# Lab 13.3 notes

## 1. What did each real dependency catch that a mock could not?

Each planted bug lives in something only the real dependency runs. I1 (`ORDER BY created_at ASC`) and I2
(`AVG` without `ROUND`) are SQL: a mocked repository would return whatever the test told it to, so the test
would pass on any SQL at all. I3 (wrong topic name) is configuration: a mocked `KafkaTemplate` would record a
`send` to `shelfie.review` and nobody would notice that nothing listens there. Only a consumer on the real
broker, subscribed to the agreed topic, sees the event is missing. Keycloak caught nothing planted, but it
is the only way the test knows real tokens (issuer, audience, signature from the realm's keys) verify.

## 2. What does Testcontainers cost?

On my machine (one run) the three containers start in about 15 seconds (Keycloak is most of it) and the whole
`mvn verify` with five tests takes about 25 seconds, against well under a second for the unit tests of
lab 13.1. So: one integration class per real dependency boundary, many unit tests for the rules. The cost
is per JVM, not per test, because the containers are static fields shared by the class.

## 3. One contract, two servers

`contract.hurl` passes on both the Module 5 server (graphql-java and a JDK HttpServer) and the Module 13
server (Spring for GraphQL): the behaviour is the same, so the contract is implementation-neutral. Writing it
showed where they differ: Module 13 trims bodies and refuses a second review of a book, Module 5 does
neither, so those rules cannot be in a shared contract. The protocol differs much more: Module 5's HTTP
audit (25 checks, GraphQL over HTTP September 2026) passes 25 of 25 on Module 5's server and 11 of 25 on
Spring's, which answers request errors with 200 or 400 instead of 422. Behaviour contracts and protocol
contracts are different files for that reason.

## 4. Black box versus the IT tests

The Hurl contract caught B1 to B3 without knowing the server is Java. It cannot see inside: it could not
tell I3 (an event on the wrong topic) from a working server, because Kafka is not part of its interface.

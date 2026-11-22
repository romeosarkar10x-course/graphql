# Lab 13.3: Integration and contract tests

Lecture: [13.3 Integration and contract tests](../../lectures/13.3-integration-contract.md). Grading: auto + rubric.
Needs Docker for Testcontainers: `bin/course test m13/03` mounts the host's Docker socket into the course
container for this lab only (lab.json `needs: ["docker"]`).

> **The Docker socket gives the container full control of your Docker daemon.** That is what Testcontainers needs
> to start PostgreSQL, Kafka and Keycloak, and it is why the course mounts it only for labs that ask. Run only
> code you trust with it: your own tests, here.

**Part A: Testcontainers.** `shelfie-it` ([contract/shelfie-it](../../contract/shelfie-it/)) is a slice of Shelfie
with real dependencies: reviews in PostgreSQL (ordering and rounding done in SQL), a `shelfie.reviews` Kafka event
per new review, and `createReview` signed by a Keycloak token (realm `shelfie`, audience `shelfie-api`, the
author is `preferred_username`). Write `src/test/java/dev/shelfie/it/*IT.java` that start all three with
Testcontainers, using the course's images (`postgres:18.7-alpine`, `apache/kafka:4.4.0`,
`quay.io/keycloak/keycloak:26.8.2` with the realm in `src/test/resources/keycloak/`), so nothing new downloads.
Failsafe runs `*IT` classes in `mvn verify`. The grader plants three bugs only a real dependency can see.

**Part B: one black-box contract, two servers.** Write `contract.hurl` (8 or more entries titled `# C01`, `# C02`
...): reads and `createReview` behaviour that the Module 5 server (graphql-java and a JDK HttpServer, lab 5.9) and
the Module 13 server (Spring for GraphQL) both pass. The grader runs it, and Module 6's 14 behaviour checks,
against both, then against three broken builds of the Module 13 server: your contract must catch each. It also
prints Module 5's 25-check HTTP audit result for both servers, for the notes.

```bash
bin/course start m13/03
cd workspace/m13-testing/03-integration-contract
COURSE_DOCKER=1 bin/course shell -c 'mvn -q verify'   # Part A by hand: the socket, then Testcontainers
bin/course test m13/03                                # both parts
```

In `notes.md`: what each real dependency caught that a mock could not, what Testcontainers costs (measure it),
what writing one contract for two servers showed (behaviour versus protocol), and what the black box cannot see.

## Done when

`bin/course test m13/03` passes and `notes.md` passes the rubric (`bin/course grade m13/03`).

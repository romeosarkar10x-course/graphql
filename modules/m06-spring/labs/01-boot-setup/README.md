# Lab 6.1 — Shelfie on Spring Boot 4

**Lecture:** [6.1 Spring for GraphQL setup](../../lectures/6.1-setup.md) · **Grading:** auto + rubric · **Needs:** nothing

The starter is a Boot 4.2 application (Spring for GraphQL 2.1) with your Module 5 schema in
`src/main/resources/graphql/`, the seed in `Data.java`, and one controller method. Port the rest.

1. Every field of `shelfie.graphqls` resolves: `ShelfieController` with `@QueryMapping`,
   `@MutationMapping` and `@SchemaMapping`. `ViewerInterceptor` (provided) puts `X-Shelfie-User` into
   GraphQLContext; read it with `@ContextValue`.
2. `application.yaml`: GraphiQL on, virtual threads on.
3. Run the load test with virtual threads on and off and fill the table in `notes.md`:

```
bin/course shell
cd workspace/m06-spring/01-boot-setup && mvn -q package -DskipTests
java -jar target/shelfie-boot.jar --spring.threads.virtual.enabled=false &
java Load.java 1000
```

`bin/course test m06/01` builds the jar, starts it on port 4000 and runs the 14 Shelfie checks every
Module 6 app must pass (`../../contract/shelfie.hurl`).

## Done when

`bin/course test m06/01` is green and `bin/course grade m06/01` gives 7/10 or more.

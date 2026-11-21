# Module 6 — Spring for GraphQL, plus DGS and Quarkus

About 10 hours, 9 lectures. By the end Shelfie runs on Spring Boot 4 and Spring for GraphQL, and you have
built the same feature in DGS and Quarkus to judge the trade-offs yourself.

## Before you start

Nothing to install. Every lab is a Maven project; `bin/course test` builds it, starts it on port 4000
inside the course container and runs Hurl against it (`contract/app-test.sh`, `contract/shelfie.hurl`: the
14 Shelfie checks every Module 6 app passes). The first build of each stack downloads its dependencies
into `.cache/m2`. Game 6b additionally pulls GraalVM (`bin/course native`, about 1 GB).

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 6.1 | [Spring for GraphQL setup](lectures/6.1-setup.md) | [m06/01 Shelfie on Spring Boot 4](labs/01-boot-setup/README.md) | auto + rubric |
| 6.2 | [Annotated controllers](lectures/6.2-controllers.md) | [m06/02 Beyond @QueryMapping](labs/02-controllers/README.md) | auto + rubric |
| 6.3 | [Schema mapping inspection](lectures/6.3-inspection.md) | [m06/03 Make the schema and the code agree](labs/03-inspection/README.md) | auto + rubric |
| 6.4 | [Interception and customisation](lectures/6.4-interception.md) | [m06/04 A tenant, from header to nested field](labs/04-interception/README.md) | auto + rubric |
| 6.5 | [Spring Data integration](lectures/6.5-spring-data.md) | [m06/05 Generated fetchers, honestly compared](labs/05-spring-data/README.md) | auto + rubric |
| 6.6 | [Exception handling](lectures/6.6-exceptions.md) | [m06/06 Six exceptions, one advice](labs/06-exceptions/README.md) | auto + rubric |
| 6.7 | [Netflix DGS](lectures/6.7-dgs.md) | [m06/07 DGS, generated from the schema](labs/07-dgs/README.md) | auto + rubric |
| 6.8 | [Quarkus and MicroProfile GraphQL](lectures/6.8-quarkus.md) | [m06/08 Code-first Shelfie on Quarkus](labs/08-quarkus/README.md) | auto + rubric |
| 6.9 | [Framework bake-off](lectures/6.9-bake-off.md) | [m06/09 The bake-off](labs/09-bake-off/README.md) | auto + rubric |

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m06/09 Bake-off leaderboard](labs/09-bake-off/README.md) | Your four apps, your table, your verdict | One shared table across machines; prizes per column |
| [m06/10 Native image race](labs/10-native-race/README.md) (rubric) | Beat 32 ms (Quarkus) and 49 ms (Spring) to the first answer | Everyone races on their own machine |
| [m06/11 A tiny Javalin GraphQL starter](labs/11-project-javalin-starter/README.md) (mini-project) | Annotation scanner + wiring + lab 5.4's HTTP layer | One owns the library, one the app |

## Changes from the plan

- **Versions:** Boot 4.2.0 / Spring for GraphQL 2.1.0 (plan: 2.0), DGS 12.1.1 (plan: 11), Quarkus platform
  3.40.1.2. **DGS 12.1.1 needs graphql-java 25.0**: on Boot 4.2's 26.1 it fails at startup
  (`NoClassDefFoundError: graphql/schema/idl/DirectiveInfo`); lab 6.7 pins 25.0.
- **6.2's projected interfaces** and **6.5's Querydsl** are explained, not built (Querydsl needs an
  annotation processor and the original line has had no release since 2024-01-29); Query by Example shows
  the same trade-off.
- **6.8's SmallRye typesafe client** is described; clients are Module 12.
- **6.4** wires `@uppercase` with a type visitor, and teaches the Mono that Spring's fetchers return.
- **Game 6b (native race)** is rubric-graded: native binaries are 60 to 100 MB, too big to keep for an
  automatic check. `bin/course native` (new) runs Maven in GraalVM CE 25.0.2.
- **Module 5's Javalin mini-project** (6.x plan) uses Javalin 7.2.4 and lab 5.4's `GraphQLHttp`.
- Every Module 6 app is checked with the same 14 Hurl checks; checkpoint-06's "bake-off table committed"
  is lab 6.9's `bakeoff.md`.

## Sources

[Spring for GraphQL reference](https://docs.spring.io/spring-graphql/reference/),
[DGS and Spring for GraphQL](https://netflix.github.io/dgs/spring-graphql-integration/),
[graphqlcodegen-maven-plugin](https://github.com/deweyjose/graphqlcodegen),
[Quarkus SmallRye GraphQL guide](https://quarkus.io/guides/smallrye-graphql).

## Checkpoint

`bin/course checkpoint 06`: labs m06/01 to m06/09 green (Shelfie on Boot 4, inspection report clean and
enforced, the bake-off table filled), plus the mini-project m06/11. Quiz: [checkpoint.md](checkpoint.md).

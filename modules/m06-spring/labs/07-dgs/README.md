# Lab 6.7 — Netflix DGS, generated from the schema

**Lecture:** [6.7 Netflix DGS](../../lectures/6.7-dgs.md) · **Grading:** auto + rubric · **Needs:** nothing

A slice of Shelfie (`src/main/resources/schema/shelfie.graphqls`) on DGS 12.1.1. DGS 12 runs on top of
Spring for GraphQL; what it adds is its own programming model and **codegen**.

1. Generate Java types and `DgsConstants` from the schema at build time. Netflix's codegen plugin is
   Gradle-only, so use the community Maven plugin, `io.github.deweyjose:graphqlcodegen-maven-plugin`
   3.11.0, into package `dev.shelfie.dgs.generated`.
2. Write `ClubFetcher` with `@DgsQuery` and `@DgsData`, returning the **generated** types and naming
   fields with `DgsConstants`.
3. The test then renames `Club.memberCount` in a copy of your schema and expects the build to fail.

`pom.xml` already pins graphql-java 25.0: DGS 12.1.1 does not start on Boot 4.2's 26.1 (notes question 2).

## Done when

`bin/course test m06/07` is green and `bin/course grade m06/07` gives 7/10 or more.

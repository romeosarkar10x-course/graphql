# Lab 6.7 notes

1. `cannot find symbol` at `.memberCount(c.memberIds().size())`: the generated builder now has
   `membersTotal(...)`. The build fails before anything runs, at the line that is wrong. Lab 6.3's
   inspection would report `Club.membersTotal` as unmapped, but only at startup, and only because we made
   startup fail; codegen moves the check to compile time, in the IDE, for every use.
2. Startup failed: `executionGraphQlService ... threw exception with message: graphql/schema/idl/DirectiveInfo`,
   a `NoClassDefFoundError`. DGS's `graphql-dgs-platform` declares graphql-java 25.0, but the Boot parent
   manages 26.1, and Maven took Boot's. Pinning `graphql-java.version` to 25.0 fixed it (Spring for
   GraphQL 2.1.0 runs on it). Two frameworks on one engine means the slower one decides the engine
   version: upgrades must wait for both, and the BOM order silently picks one.
3. Generated types: always match the schema, refactor-safe constants for field names; but they are
   anemic DTOs, so every response is copied from domain objects (my `toGraphQL` methods), and the build
   needs a plugin. Own records: no mapping layer and they can carry the ids nested fetchers need
   (`ownerId`); but nothing stops them drifting from the schema except inspection.
4. The typed client and query projections generated from the schema (testing and service-to-service
   calls), and federation support from Netflix's own production use (Module 14), plus `@DgsData` style
   for teams coming from older DGS. Shelfie does not need them yet: one service, no federation, and
   Spring's own `HttpGraphQlTester` covers testing.

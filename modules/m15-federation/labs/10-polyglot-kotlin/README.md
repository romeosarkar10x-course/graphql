# Mini-project 15.10: Polyglot preview, accounts in Kotlin

Lecture: [15.4 Java subgraphs](../../lectures/15.4-java-subgraphs.md). Grading: auto. Needs the federation tools
(`bin/course fed`).

The accounts team wants Kotlin. The starter is a plain GraphQL server in Kotlin with
[graphql-kotlin](https://github.com/ExpediaGroup/graphql-kotlin) 11.0.0 (Expedia, Apache-2.0), schema generated
from Kotlin classes, built with Maven (`kotlin-maven-plugin`, shaded jar). Make it Shelfie's accounts subgraph:

- the same API as the Java one ([accounts.graphqls](../../contract/subgraphs/src/main/resources/graphql/accounts/accounts.graphqls)):
  `me` from the `X-Shelfie-User` header (null without it), `user(id:)`, `users`; `User @key(fields: "id")` with a
  `@shareable` `handle`;
- federated (`toFederatedSchema`, an entity resolver for `User`; unknown ids resolve to null);
- composing with Java's catalog, reviews and clubs, unchanged.

The test runs your jar on :4001 and the contract's three Java subgraphs, composes from your `_service` SDL, routes
through Apollo Router, and checks the mix (`tests/polyglot.hurl`): Java's reviews and clubs pointing at your
users, your users carrying fields Java adds.

```bash
bin/course start m15/10
cd workspace/m15-federation/10-polyglot-kotlin
bin/course shell -c 'mvn -q package && (java -jar target/shelfie-accounts-kotlin.jar &) && SKIP=accounts bash /course/modules/m15-federation/contract/stack.sh'
bin/course test m15/10
```

Solo only: one subgraph, one grader. A group can split it (generator, entity resolver, HTTP), but the grader
checks one jar.

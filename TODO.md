# TODO.md — remaining work

## Now: user review of Modules 3 and 4

The user asked (2026-11-21) for two modules, then stop. Modules 3 and 4 are built; wait for review
before Module 5. Earlier answers still hold: single-file Java for small clients, a generous rubric share.

## Verification debt

- [ ] PayPal "700 ms p99 per round trip" and "3 to 52 products in 2 years": the Medium post blocks
      automated fetches (403). Verify by hand or replace with another source (lecture 1.2, case-studies tab).
- [ ] Re-verify public API answers in m00/03 and the GitHub schema answers in m00/06 each term.
- [ ] With a GITHUB_TOKEN: run m02/09 bonus holes and m02/12's live check; record the CLI query's
      `rateLimit { cost }` in m02/12's reference notes. Confirm the reference notes' claims that GitHub
      REST has no pinned-items endpoint and that contributions are GraphQL-only.
- [ ] Refresh `tools/schemas/github-*.graphql` and the m02/06 public API type counts each term.
- [ ] Open `/voyager.html` in a browser once (game 3c): only the CDN assets and the JS API names were
      checked with curl, not the rendering.
- [ ] Re-check graphql-java-extended-validation for a release built against graphql-java 26 and Hibernate
      Validator 9; lift the HV 8 pin if so.
- [ ] Re-check MinIO image availability; the course uses SeaweedFS 4.54 meanwhile.
- [ ] Game 4b signatures: refresh from the GitHub snapshot and AniList introspection each term.
- [ ] Check whether a newer graphql-java fixes the subscription-root `@skip` gap and parses operation
      descriptions; update lectures 2.5 and 2.7 and the module README table if so.

## Next modules (in order)

- [ ] M5 graphql-java from scratch (the Shelfie Maven project starts here: `./mvnw verify`; 40 Hurl tests)
- [ ] M6–M18, capstone, per plan/syllabus.md
- [ ] Update Module 6.7 text for DGS 12 and the Maven codegen plugin (VERSIONS.md)

## Infra backlog

- [ ] PostgreSQL + Testcontainers in compose from Module 6/9
- [ ] A per-module Shelfie checkpoint repo layout once learners build their own server (Module 5)

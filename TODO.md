# TODO.md — remaining work

## Now: user review of Module 2

Answered 2026-11-21: single-file Java for small clients is fine; more rubric parts wanted (added to
m01/01, m01/04, new m01/09). Keep a generous rubric share in later modules.

## Verification debt

- [ ] PayPal "700 ms p99 per round trip" and "3 to 52 products in 2 years": the Medium post blocks
      automated fetches (403). Verify by hand or replace with another source (lecture 1.2, case-studies tab).
- [ ] Re-verify public API answers in m00/03 and the GitHub schema answers in m00/06 each term.
- [ ] With a GITHUB_TOKEN: run m02/09 bonus holes and m02/12's live check; record the CLI query's
      `rateLimit { cost }` in m02/12's reference notes. Confirm the reference notes' claims that GitHub
      REST has no pinned-items endpoint and that contributions are GraphQL-only.
- [ ] Refresh `tools/schemas/github-*.graphql` and the m02/06 public API type counts each term.
- [ ] Check whether a newer graphql-java fixes the subscription-root `@skip` gap and parses operation
      descriptions; update lectures 2.5 and 2.7 and the module README table if so.

## Next modules (in order)

- [ ] M3 Type system and SDL (records-to-SDL generator; nullability lab)
- [ ] M4 Mutations and input design (chaos harness; undo tokens)
- [ ] M5 graphql-java from scratch (the Shelfie Maven project starts here: `./mvnw verify`; 40 Hurl tests)
- [ ] M6–M18, capstone, per plan/syllabus.md
- [ ] Update Module 6.7 text for DGS 12 and the Maven codegen plugin (VERSIONS.md)

## Infra backlog

- [ ] PostgreSQL + Testcontainers in compose from Module 6/9
- [ ] A per-module Shelfie checkpoint repo layout once learners build their own server (Module 5)

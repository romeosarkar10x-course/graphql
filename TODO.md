# TODO.md — remaining work

## Now: user review of Module 9

The user said "continue" (2026-11-21) after Module 8; Module 9 is built. Wait for review before Module 10.

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
- [ ] Re-check DGS for a release built on graphql-java 26; lift lab 6.7's 25.0 pin if so.
- [ ] Re-check the Quarkus 4.0 platform BOM (4.0.0.CR1 on 2026-11-21); move lab 6.8 when it is final.
- [ ] Game 6b numbers came from one 24-core machine; JVM build times for 6b were not timed.
- [ ] m05/11 bug B1 (common pool) is only detectable with parallelism > 1; fine on any multi-core machine.
- [ ] Re-run lab 5.4's audit against Spring for GraphQL each release (11/25 on 2.1.0).
- [ ] Check whether a newer graphql-java fixes the subscription-root `@skip` gap and parses operation
      descriptions; update lectures 2.5 and 2.7 and the module README table if so.

- [ ] Game 7b cards: re-check AniList's `UpdateUser` (19 args), `Media` (75 args), `Json` fields and the
      GitHub counts (73 `databaseId`, 253 of 298 inputs with `clientMutationId`, 147 `Repository` fields).
- [ ] graphql-inspector: move off 7.0.2 once a 7.0.x has been stable two weeks; recheck lab 7.6's two
      tool disagreements (optional argument, deprecation) after any upgrade.
- [ ] RFC stages in Module 7 (ObjectIdentification, SemanticNullability, #1163, #1231, #997) each term.
- [ ] Lab 7.1's SDL-only experiment: the reference notes describe a good answer; no measured run.
- [ ] PostgreSQL 19.0 (released 2026-11-19): move the course service off 18.7 next term; re-run lab 8.2's
      plans (the OR-form filter vs row-value seek) on it.
- [ ] Re-check Spring Data JPA's keyset scroll SQL (OR form, 4.2.0) and Spring for GraphQL's cursor
      strategy bean condition and status codes (400/200) each release; update lectures 8.2 and 8.5.
- [ ] RFC stages in Module 8 (#1163 RFC 1, #1183 RFC 3, #1184 RFC 2, #1073 RFC 3, #1200 RFC 0) each term.
- [ ] Module 9 timing checks (game 9a's p95 at half the baseline, lab 9.6's 90% hit rate, lab 9.7's JMH 2x,
      lab 9.8's winner re-run) were green on one machine; watch them on slow or busy machines.
- [ ] Lab 9.8's reference report has one run per configuration; repeat 5x and report medians.
- [ ] Re-check each release: Spring for GraphQL enabling DataLoader chaining (2.1.0: never), sending
      Cache-Control on GET (2.1.0: none), the GET-mutation `Allow` header; graphql-java's APQ hash check on hits.
- [ ] Module 7's CI workflow pins actions/checkout@v4 and setup-java@v4; current majors are v7 and v6.
- [ ] Mini-project 8 and game 8a time-based checks (bot every 20/30 ms) were green on one machine;
      watch for flakiness on slow machines.

## Next modules (in order)

- [ ] M10–M18, capstone, per plan/syllabus.md

## Infra backlog

- [ ] Testcontainers needs a Docker socket in the course container (PostgreSQL itself is a course service since Module 8)
- [ ] A per-module Shelfie checkpoint repo layout once learners build their own server (Module 5)

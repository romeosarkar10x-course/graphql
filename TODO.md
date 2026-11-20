# TODO.md — remaining work

## Now: user review of infra + Module 0 + Module 1 (CONTEXT.md §2.3)

Open questions for the user:
- Is the lab format (single-file Java for small clients, Maven projects for bigger ones) right?
- Rubric-lab volume: Module 1 has 5 rubric parts. More, fewer?

## Verification debt

- [ ] PayPal "700 ms p99 per round trip" and "3 to 52 products in 2 years": the Medium post blocks
      automated fetches (403). Verify by hand or replace with another source (lecture 1.2, case-studies tab).
- [ ] Re-verify public API answers in m00/03 and the GitHub schema answers in m00/06 each term.

## Next modules (in order)

- [ ] M2 Query language, driven from Java (8 lectures; GitHub profile CLI with picocli; analyzer with graphql-java `Parser`)
- [ ] M3 Type system and SDL (records-to-SDL generator; nullability lab)
- [ ] M4 Mutations and input design (chaos harness; undo tokens)
- [ ] M5 graphql-java from scratch (the Shelfie Maven project starts here: `./mvnw verify`; 40 Hurl tests)
- [ ] M6–M18, capstone, per plan/syllabus.md
- [ ] Update Module 6.7 text for DGS 12 and the Maven codegen plugin (VERSIONS.md)

## Infra backlog

- [ ] PostgreSQL + Testcontainers in compose from Module 6/9
- [ ] A per-module Shelfie checkpoint repo layout once learners build their own server (Module 5)

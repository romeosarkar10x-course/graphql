# Module 4 — Mutations and input design

About 6 hours, 6 lectures. By the end your mutations are safe to retry, honest about failure and easy
for clients to cache.

## Before you start

`bin/course ref up --rebuild`. The reference server gained mutations (reviews, memberships, cover
uploads), an `X-Shelfie-User` header for "who is calling", and a `storage` service (SeaweedFS, S3 API on
:8333) for pre-signed uploads. `bin/course test` puts the reference data back to the seed before every
lab; `bin/course ref reset` does it by hand.

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 4.1 | [Mutation basics](lectures/4.1-mutation-basics.md) | [m04/01 First mutations, serial vs parallel](labs/01-mutation-basics/README.md) | auto + rubric |
| 4.2 | [Inputs and payloads](lectures/4.2-inputs-payloads.md) | [m04/02 Redesign five mutations](labs/02-inputs-payloads/README.md) | auto + rubric |
| 4.3 | [Errors as data](lectures/4.3-errors-as-data.md) | [m04/03 Two error styles, one Java type](labs/03-errors-as-data/README.md) | auto + rubric |
| 4.4 | [Idempotency, concurrency, bulk](lectures/4.4-idempotency.md) | [m04/04 Safe to retry, safe to race](labs/04-idempotency/README.md) | auto + rubric |
| 4.5 | [Input validation](lectures/4.5-input-validation.md) | [m04/05 Where does each rule belong?](labs/05-validation/README.md) | auto + rubric |
| 4.6 | [File uploads](lectures/4.6-file-uploads.md) | [m04/06 A 50 MB cover, two ways](labs/06-uploads/README.md) | auto + rubric |

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m04/07 Chaos button](labs/07-chaos-button/README.md) | Survive 5 seeds of lost responses, duplicates and races | Write chaos modes that break each other's services |
| [m04/08 Undo everything](labs/08-project-undo/README.md) (mini-project) | `undoToken` and `undo(token)` with the command pattern | One writes commands, the other writes scripts that try to break them |
| [m04/09 Design critique cards](labs/09-critique-cards/README.md) | Rate 8 real signatures (GitHub, AniList, Shelfie) on 5 criteria | Reveal card by card, argue the 2-point gaps |

## Changes from the plan

- 4.4 uses **JDBC with a version column** instead of JPA `@Version` (the same SQL, visible); JPA comes
  with Spring in Module 6.
- 4.6 uses **SeaweedFS** instead of MinIO (MinIO stopped publishing container images in 2025;
  `_workbench/2026-11-21-m04-storage/`), and the reference server implements the multipart convention
  itself (Spring for GraphQL has none) instead of DGS or multipart-spring-graphql.
- 4.5 pins **Hibernate Validator 8.0.6** with graphql-java-extended-validation 24.0: version 9 breaks it.

## Spec references

[Normal and Serial Execution](https://spec.graphql.org/September2025/#sec-Normal-and-Serial-Execution),
[Mutation](https://spec.graphql.org/September2025/#sec-Mutation),
[Input Objects](https://spec.graphql.org/September2025/#sec-Input-Objects);
the [GraphQL multipart request convention](https://github.com/jaydenseric/graphql-multipart-request-spec)
(community spec, not part of GraphQL).

## Checkpoint

`bin/course checkpoint 04`: the mutation SDL with payloads and typed errors (m04/02, m04/03), the
idempotency harness (m04/04) and the chaos tests (m04/07) green, plus m04/01, m04/05, m04/06 and the undo
project. Quiz: [checkpoint.md](checkpoint.md).

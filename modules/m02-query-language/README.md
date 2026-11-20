# Module 2 — The query language, driven from Java

About 7 hours, 8 lectures. By the end you write any read operation fluently by hand and can treat a
GraphQL document as data in Java.

## Before you start

`bin/course ref up --rebuild` (the reference schema grew for this module: `books(titleContains:, first:)`,
the `search` union and a `Subscription` type).

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 2.1 | [Anatomy of a query](lectures/2.1-anatomy.md) | [m02/01 Responses as Java records](labs/01-anatomy/README.md) | auto + rubric |
| 2.2 | [Arguments and variables](lectures/2.2-variables.md) | [m02/02 Variables vs injection](labs/02-variables/README.md) | auto + rubric |
| 2.3 | [Aliases and multiple root fields](lectures/2.3-aliases.md) | [m02/03 Two clubs, one request](labs/03-aliases/README.md) | auto + rubric |
| 2.4 | [Fragments](lectures/2.4-fragments.md) | [m02/04 Refactor without changing a byte](labs/04-fragments/README.md) | auto + rubric |
| 2.5 | [Directives in operations](lectures/2.5-directives.md) | [m02/05 One query, two cards](labs/05-directives/README.md) | auto + rubric |
| 2.6 | [Introspection](lectures/2.6-introspection.md) | [m02/06 Dump any schema to SDL](labs/06-introspection/README.md) | auto + rubric |
| 2.7 | [Documents and hygiene](lectures/2.7-documents.md) | [m02/07 12 named operations](labs/07-documents/README.md) | auto + rubric |
| 2.8 | [Documents as data](lectures/2.8-documents-as-data.md) | [m02/08 A query analyzer](labs/08-analyzer/README.md) | auto + rubric |

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m02/09 GraphQL golf](labs/09-golf/README.md) | Beat par (234) on 8 holes, plus 2 GitHub bonus holes | 15 minutes, lowest strokes wins |
| [m02/10 Predict the response](labs/10-predict/README.md) | 10 cards, 10/10 | Reveal card by card, argue the misses |
| [m02/11 Fix the broken query](labs/11-fix-broken/README.md) | 12 real validation errors in under 10 minutes | Race, then write broken queries for each other |
| [m02/12 GitHub profile CLI](labs/12-project-github-cli/README.md) (mini-project) | picocli + Maven, one request | Build alone, compare cards and query costs |

## Tools added in this module

- `bin/course gql validate <doc> [--schema <file|url>]`: validate without executing (works for
  subscriptions and for GitHub's schema in `tools/schemas/`).
- `bin/course gql operations <doc>`: list a document's operations and the features they use.
- `bin/course gql schema-lines <sdl>`: a schema as sorted one-line facts, for diffing.
- `bin/course shell -c '<cmd>'`: run one command inside the course image.

## Where implementations and the spec disagree

Found while building this module (graphql-java 26.1, probes in `_workbench/2026-11-21-m02-probe/`):

| September 2025 spec | graphql-java 26.1 |
|---|---|
| Operations may have descriptions (RFC #1170) | Parser rejects them |
| `@skip`/`@include` forbidden on a subscription's root field | Accepted; with a variable the validator throws a `NullPointerException` |
| Fragment arguments | Not in the spec yet (RFC #1224); parser rejects them |

## Spec references

[Language](https://spec.graphql.org/September2025/#sec-Language),
[Selection Sets](https://spec.graphql.org/September2025/#sec-Selection-Sets),
[Fragments](https://spec.graphql.org/September2025/#sec-Language.Fragments),
[Directives](https://spec.graphql.org/September2025/#sec-Language.Directives),
[Field Selection Merging](https://spec.graphql.org/September2025/#sec-Field-Selection-Merging),
[Single Root Field](https://spec.graphql.org/September2025/#sec-Single-Root-Field);
RFCs [#1170 operation descriptions](https://rfcs.graphql.org/rfcs/1170/),
[#976 request extensions](https://rfcs.graphql.org/rfcs/976/),
[#1224 fragment arguments](https://rfcs.graphql.org/rfcs/1224/).

## Checkpoint

[checkpoint.md](checkpoint.md), then `bin/course checkpoint 02`: the 12 operations (lab 2.7), the
analyzer (lab 2.8) and the CLI (lab 2.12) must pass.

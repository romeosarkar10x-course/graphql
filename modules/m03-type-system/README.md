# Module 3 — The type system and SDL with graphql-java

About 8 hours, 9 lectures. By the end you can model any domain in SDL, wire it to Java types, and
predict exactly how values are coerced.

## Before you start

Nothing to start: every lab in this module runs graphql-java **in-process** (`bin/course java`), no
server. Game 3c needs `bin/course ref up` and a browser. `tools/course-lib` gained
graphql-java-extended-scalars 24.0 (the course image rebuilds the class path on the next run).

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 3.1 | [SDL and schema building](lectures/3.1-sdl-and-building.md) | [m03/01 Build a schema by hand](labs/01-schema-building/README.md) | auto + rubric |
| 3.2 | [Built-in scalars, Java-side](lectures/3.2-scalars.md) | [m03/02 Long IDs, safely](labs/02-scalars/README.md) | auto + rubric |
| 3.3 | [Custom scalars](lectures/3.3-custom-scalars.md) | [m03/03 An ISBN scalar, both input paths](labs/03-custom-scalars/README.md) | auto + rubric |
| 3.4 | [Nullability and lists](lectures/3.4-nullability.md) | [m03/04 Where does null land?](labs/04-nullability/README.md) | auto + rubric |
| 3.5 | [Enums](lectures/3.5-enums.md) | [m03/05 Enums inside and out](labs/05-enums/README.md) | auto + rubric |
| 3.6 | [Interfaces](lectures/3.6-interfaces.md) | [m03/06 Interfaces and type resolvers](labs/06-interfaces/README.md) | auto + rubric |
| 3.7 | [Unions and Java sealed types](lectures/3.7-unions-sealed.md) | [m03/07 A feed as a sealed union](labs/07-unions-sealed/README.md) | auto + rubric |
| 3.8 | [Input types and `@oneOf`](lectures/3.8-inputs-oneof.md) | [m03/08 `BookLookup @oneOf`](labs/08-oneof/README.md) | auto + rubric |
| 3.9 | [Directives, extensions, deprecation, coordinates](lectures/3.9-extensions-coordinates.md) | [m03/09 The full Shelfie schema, split five ways](labs/09-modular-schema/README.md) | auto + rubric |

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m03/10 Wireframe to schema](labs/10-wireframe/README.md) | 4 screens in, SDL and 4 operations out; compare with the model answer | Design alone, diff schemas, argue each difference |
| [m03/11 Nullability bets](labs/11-nullability-bets/README.md) | 16 points on 8 fields; score 8+ | Reveal field by field; highest total wins |
| [m03/12 Voyager reveal](labs/12-voyager-reveal/README.md) | Sketch the schema from memory, then open Voyager | Score each other's sketches |
| [m03/13 Records to SDL](labs/13-project-records-to-sdl/README.md) (mini-project) | A code-first generator in about 200 lines, Maven | Swap fixtures that break each other's generator |

## Tools added in this module

- `bin/course gql coordinates <schema> <coordinate>...`: resolve schema coordinates.
- `bin/course gql stats <schema>`: type count and deprecations per kind.
- `<schema>` may now be a **directory**: every `*.graphqls` in it, merged.
- <http://localhost:8080/voyager.html>: GraphQL Voyager on the reference schema (game 3c).

## Where graphql-java 26.1 and the spec meet

Probes in `_workbench/2026-11-21-m03-probe/`:

| Topic | graphql-java 26.1 |
|---|---|
| `@oneOf` (September 2025) | Supported; literal `{id: null}` fails at execution, variable cases at variable coercion |
| OneOf inhabitability (RFC #1211) | Enforced at schema build |
| Deprecated arguments, input fields, enum values | Supported; required ones rejected |
| Interfaces implementing interfaces | Supported; transitive interfaces must be listed |
| Directives on directive definitions (RFC #1206, accepted 2026) | Parser rejects them |
| extended-scalars 24.0 / extended-validation 24.0 | Work on 26.1 (extended-validation needs Hibernate Validator 8, not 9) |

## Spec references

[Type System](https://spec.graphql.org/September2025/#sec-Type-System),
[Schema Coordinates](https://spec.graphql.org/September2025/#sec-Schema-Coordinates),
[OneOf Input Objects](https://spec.graphql.org/September2025/#sec-OneOf-Input-Objects);
RFCs [#825 OneOf](https://rfcs.graphql.org/rfcs/825/), [#1211 OneOf inhabitability](https://rfcs.graphql.org/rfcs/1211/),
[#794 schema coordinates](https://rfcs.graphql.org/rfcs/794/), [#525 deprecating input values](https://rfcs.graphql.org/rfcs/525/),
[#1206 directives on directives](https://rfcs.graphql.org/rfcs/1206/), [#649 `@specifiedBy`](https://rfcs.graphql.org/rfcs/649/),
[#373 interfaces implementing interfaces](https://rfcs.graphql.org/rfcs/373/).

## Checkpoint

`bin/course checkpoint 03`: labs m03/01 to m03/09 and the generator (m03/13) pass; m03/09 is the full
Shelfie SDL (15+ types). Quiz: [checkpoint.md](checkpoint.md).

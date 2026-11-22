# Lab 17.6: Introspection and printing

Lecture: [17.6 Introspection and printing](../../lectures/17.6-introspection.md). Grading: auto.

Every schema already contains the introspection types (`Schema.BUILT_INS`, written in SDL). Two parts are missing:

- `Introspection.resolve`: the resolvers for `__schema`, `__type(name:)` and every field of `__Schema`, `__Type`,
  `__Field`, `__InputValue`, `__EnumValue` and `__Directive` (section 4.2), over the schema's own records;
- `Printer.schema`: the schema as SDL, the user's types and directives only.

The test runs four round trips on three schemas (the reference server's, the conformance fixture and
`contract/printing/kitchen-sink.graphqls`, which has every type-system feature):

| Check | |
| --- | --- |
| print | your SDL describes the same schema as the original (both printed by graphql-java's `SchemaPrinter`) |
| stable | printing your SDL again gives the same text |
| introspect | your introspection result equals graphql-java's for the user's types and the spec's directives |
| rebuild | SDL rebuilt from your introspection equals SDL rebuilt from graphql-java's |

Where graphql-java differs from the spec, the comparison leaves it out and says why (`RoundTrip.java`): its
built-in descriptions, its extra `@defer` and `@experimental_disableErrorPropagation` directives,
`includeDeprecated` as a nullable `Boolean`, `{text : "a"}` as a default value, and the order of `possibleTypes`.

```bash
bin/course start m17/06
bin/course test m17/06
```

## Experiment

SDL to schema to introspection to SDL: diff your printer's output with the SDL graphql-java rebuilds from your
introspection. What can introspection not carry back?

## Done when

`bin/course test m17/06` passes.

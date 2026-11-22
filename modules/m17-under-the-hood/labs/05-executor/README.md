# Lab 17.5: Execution on virtual threads

Lecture: [17.5 Execution](../../lectures/17.5-execution.md). Grading: auto.

`Executor.execute` returns an error. Write the spec's execution algorithms (section 6), one method each:
`GetOperation`, `CoerceVariableValues`, `ExecuteCollectedFields` (called `ExecuteSelectionSet` before the September
2025 edition), `CollectFields` with `@skip` and `@include`, `ExecuteField`, `CoerceArgumentValues`, `CompleteValue`
with `ResolveAbstractType`, and the input and result coercion of the built-in scalars, enums, lists (a single value
becomes a list of one) and input objects (defaults, `@oneOf`). Handle errors as 6.4.4 says: a field error makes its
position null, and a null in a non-null position propagates to the nearest nullable parent.

Field values come from a `Resolver`; `__typename`, `__schema` and `__type` from `Introspection.resolve` (provided).
Query fields, and the items of a list, run in parallel on virtual threads; mutation root fields run one after
another.

The test runs:

- **30 conformance tests** (`contract/conformance/*.json`), on the reference server's data (its own `Store`) or on a
  small fixture schema whose resolvers throw, echo their arguments and log. The expected responses come from
  graphql-java 26.1, except where it leaves `locations` off an error the spec says should have them (each such test
  says so in `graphqlJava`). Data must be equal; errors are compared by path and locations, not by message;
- **parallelism**: seven resolvers of 200 ms each must finish in under 500 ms, on virtual threads;
- **Module 2's twelve operations**, over the reference data: your responses must equal graphql-java's.

```bash
bin/course start m17/05
bin/course test m17/05
```

## Experiment

Make `CompleteValue` run list items one after another and run the parallelism check again. Then change the root
fields of a mutation to run in parallel: which conformance test fails, and why is it right to?

## Done when

`bin/course test m17/05` passes.

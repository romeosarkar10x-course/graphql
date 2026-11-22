# graphql-java internals (26.1, reference notes)

Line numbers are from tag `v26.1` (commit 722262d, 2026-08-24).

## Five classes

### graphql.parser.Parser

Spec 2 and appendix C. `parseDocument(ParserEnvironment)` runs an ANTLR 4 lexer and parser generated from
`src/main/antlr/*.g4` (shaded into `graphql.org.antlr`), then `GraphqlAntlrToLanguage` (936 lines) walks ANTLR's
parse tree into graphql-java's immutable AST (`graphql.language.*`, builders). Ours is a hand-written lexer and
recursive descent straight into records. The difference: a generated parser is easy to keep in step with a
grammar, but it inherits ANTLR's behaviour where the grammar files differ from the spec (the U+2028 rule, `*`
instead of "at least one", backtracking past `[lookahead != {]`). `ParserOptions` adds limits our parser lacks:
characters (1 MB by default), tokens (15,000), whitespace tokens and rule depth, against huge documents.

### graphql.validation.OperationValidator

Spec 5. 1,958 lines: every rule in one class that walks the document once (it implements `DocumentVisitor`); there
is no class per rule in 26.1 (`graphql/validation/` has no `rules` package). Rules are an enum, `OperationValidationRule`, and
`Validator.validateDocument` takes a predicate to run only some. Ours walks the tree once per rule. One walk is
faster on big documents; the cost is a class where every rule shares state, which is why error counts for cycles
and overlaps follow its traversal rather than anything in the spec.

### graphql.execution.ExecutionStrategy

Spec 6.3 and 6.4. `resolveFieldWithInfo` (line 362) is ExecuteField: `fetchField` (411) calls the DataFetcher,
`completeField` (633) and `completeValue` (688) are CompleteValue, with `completeValueForList` (770),
`completeValueForScalar` (891), `completeValueForEnum` (918) and `completeValueForObject` (944).
`AsyncExecutionStrategy` (99 lines) runs a selection set's fields concurrently through `CompletableFuture`s; ours
starts a virtual thread per field and blocks on it. Futures let a DataFetcher return without a thread (and let the
DataLoader dispatch batches when all fields of a level are waiting); virtual threads make blocking cheap but need a
thread per pending field. Non-null handling: `NonNullableFieldValidator` throws `NonNullableFieldWasNullException`; it
is caught where the parent completes (`ExecutionStrategy` lines 774 and 862,
`AbstractAsyncExecutionStrategy.handleNonNullException`, and `Execution` line 233 for the root): the same shape as
our `Bubble`.

### graphql.execution.ValuesResolver

Spec 6.1.2 CoerceVariableValues and 6.4.1 CoerceArgumentValues: `coerceVariableValues` and `getArgumentValues`.
It also produces "normalized" values (literals with their types, `getNormalizedVariableValues`, used by
`graphql.normalized`, the ExecutableNormalizedOperation). Ours has one
coercion path for literals and one for variables; graphql-java distinguishes external (JSON), internal (Java) and
literal (AST) values (`ValuesResolverConversion.externalValueToInternalValueImpl`, `externalValueToLiteral`),
because custom scalars convert between all three.

### graphql.introspection.Introspection

Spec 4. The `__Schema`, `__Type`, `__Field`... types are built in Java with `GraphQLObjectType.newObject()` and
data fetchers per field, plus the meta-fields' definitions (`SchemaMetaFieldDef`, `TypeMetaFieldDef`,
`TypeNameMetaFieldDef`), with data fetchers registered in the code registry (line 175). Ours writes the types as SDL
and resolves them from the schema's records. Built in Java, the built-ins carry graphql-java's own descriptions
(the scalars' "Built-in Int" are in `graphql.Scalars`) and the extra directives it supports (`@defer`,
`@experimental_disableErrorPropagation`): that is where our round trips differ.

## Tests

`./gradlew test --tests 'graphql.parser.*'`: not run while writing this module (see the note in the README);
record your own count and time.

## The one-line change

```diff
-        String[] lines = rawValue.split("\\n");
+        String[] lines = rawValue.split("\\r\\n|[\\n\\r]", -1);
```

The `-1` keeps trailing empty lines, which `split` otherwise drops (the code after it trims them itself). The new
case: `removeIndentation("\r\n    one\r\n    two\r\n")` is `"one\ntwo"`.

## Building and using it

`RELEASE_VERSION=26.1-local ./gradlew publishToMavenLocal -PpublishToMavenLocal`, then
`com.graphql-java:graphql-java:26.1-local` in a pom. Not run while writing this module: unverified.

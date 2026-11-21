# Hints, lab 7.6

1. Think about two kinds of client: one sending **operations** (any argument or input change can make
   them invalid) and one reading **responses** (any output change can make a value it relied on
   disappear or turn null). An output getting stricter (`Book` to `Book!`) helps the reader; an input getting
   stricter (`Int` to `Int!`) hurts the sender.
2. `SchemaDiffSet.diffSetFromSdl(oldSdl, newSdl)`, a `CapturingReporter`, and
   `new SchemaDiff().diffSchema(set, reporter)`; then `reporter.getBreakages()` and `getDangers()`, each
   `DiffEvent` with `getReasonMsg()`.
3. For RFC #1231: `Parser.parse(sdl)` gives a `Document`; `transform` each `ObjectTypeDefinition` with only
   the `FieldDefinition`s where `!hasDirective("deprecated")` (and the same for `EnumTypeDefinition` values),
   then `AstPrinter.printAst` and `UnExecutableSchemaGenerator.makeUnExecutableSchema` on the result.
   Building a type with no fields throws.

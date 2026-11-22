# Hints: 16.2 field usage

1. `beginExecution` in a `SimplePerformantInstrumentation` can return an `InstrumentationContext` whose
   `onCompleted` sees the `ExecutionResult`. Note the start time when it begins and the end time when it
   completes. The parameters give you the query, the operation name, the variables, the schema and the
   GraphQL context.
2. graphql-java's `QueryTraverser` walks a document against the schema (it needs the operation name and the
   variables). Its visitor's `visitField` gets the field's parent type (`getFieldsContainer()`), the field
   definition and the arguments written in the document. Collect into a set to keep each coordinate once.
   A document that fails to parse makes the traverser throw; catch that and report nothing.
3. Keep the map and the operations in memory behind one lock, and let a scheduled task send and clear them every
   second. Keep them when the POST fails, so the next try sends them. For `Unused.java`: list every field of every
   object type in the SDL (graphql-java's `SchemaParser` gives you a `TypeDefinitionRegistry`). Then remove each
   coordinate that some operation inside the window used, and remember each operation carries its own timestamp.

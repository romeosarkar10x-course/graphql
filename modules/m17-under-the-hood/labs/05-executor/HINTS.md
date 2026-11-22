# Hints: 17.5 execution

1. Write the algorithms in the spec's order and with its names; each is short. Start with GetOperation and
   CoerceVariableValues: request errors there mean no `data` key at all. `CollectFields` returns a map in
   document order, keyed by response name.
2. For null propagation, let `CompleteValue` throw a private exception when a non-null position is null (after
   recording the error once); catch it where the type is nullable and return null there. The error goes in
   `errors` with the path and the field's location.
3. For parallel fields, submit each field to an `Executors.newVirtualThreadPerTaskExecutor()` and then `join` the
   futures in order, so the response keeps document order. List items the same way. Mutation root fields run in a
   plain loop.

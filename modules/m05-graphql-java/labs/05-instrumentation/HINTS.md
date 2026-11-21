# Hints, lab 5.5

1. `SimplePerformantInstrumentation` has a do-nothing version of every hook; override only the ones you
   need. A hook that starts something returns an `InstrumentationContext`, and
   `SimpleInstrumentationContext.whenCompleted((value, error) -> ...)` runs when that something finishes,
   async fetchers included.
2. `createStateAsync` is called once per execution; whatever it returns is passed as the `state`
   argument to every other hook of that execution. The field's coordinate is
   `getExecutionStepInfo().getObjectType().getName()` + "." + the field definition's name.
3. `instrumentExecutionResult` sees the finished `ExecutionResult` and the state:
   `result.transform(b -> b.addExtension("timing", ...))` returns a copy with your extension.

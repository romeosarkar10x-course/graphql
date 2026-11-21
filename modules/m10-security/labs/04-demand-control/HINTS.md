# Hints, lab 10.4

1. Limits that need no resolver run before execution: `QueryComplexityLimits` during validation, the
   cost limit at `beginExecuteOperation`. A `WebGraphQlInterceptor` can put per-request values into the
   `GraphQLContext` with `request.configureExecutionInput((input, b) -> b.graphQLContext(...).build())`.
2. `FieldComplexityCalculator` is `(env, childCost) -> int`. `env.getArguments()` holds the field's
   arguments with defaults applied, so `first` is always there for Shelfie's lists. Price a list as
   `first * (own + childCost)`, and watch out for zero and negative pages.
3. For the budget, compute the same cost yourself with `QueryComplexityCalculator.newCalculator()` from the
   `ExecutionContext` in `beginExecuteOperation`, then `bucket.tryConsumeAndReturnRemaining(cost)`. To refuse,
   throw `AbortExecutionException` with a `GraphQLError` whose extensions carry the code. For the timeout,
   `beginExecution` gets the real `ExecutionInput`: schedule its `cancel()`.

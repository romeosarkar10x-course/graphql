# Hints: m11/01

1. A subscription resolver returns a `Flux`. Something must emit into a shared `Flux` when `sendMessage`
   runs, and each subscriber needs only its own room's events. Look at what `Bus` already offers.
2. `bus.messages(roomId)` is the source stream; return it from `messageSent`. For the error, Spring hands
   exceptions from the source stream to a `SubscriptionExceptionResolver`, not to the
   `DataFetcherExceptionResolver` that `Errors` extends.
3. Extend `SubscriptionExceptionResolverAdapter` as a `@Component`, override `resolveToSingleError(Throwable)`
   and build the error with `GraphqlErrorBuilder.newError()`; put `code` in `extensions`. Return `null`
   for exceptions you do not recognise so Spring keeps its default.

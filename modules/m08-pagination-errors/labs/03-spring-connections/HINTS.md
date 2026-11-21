# Hints, lab 8.3

1. Three Spring types do the work. `CursorStrategy<ScrollPosition>` turns a `KeysetScrollPosition`'s
   key map into text and back (`ScrollPosition.forward(Map.of("published", p, "id", id))`).
   `CursorStrategy.withEncoder(strategy, CursorEncoder.noOpEncoder())` returns the
   `EncodingCursorStrategy` Boot looks for. `@GraphQlExceptionHandler` methods in a `@ControllerAdvice` return a
   `GraphQLError` built with `GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST)`.
2. If you let `CursorEncoder.base64()` decode, bad base64 surfaces as a plain `IllegalArgumentException`
   from inside Spring, and a handler for that type would catch every bug in your app. Do the base64 in
   your own strategy and throw one exception type of your own for every malformed cursor. Exceptions
   thrown while resolving `ScrollSubrange` still reach your `@GraphQlExceptionHandler`.
3. In the controller: `subrange.forward()` tells you the direction and `subrange.position()` the keys
   (already flipped to backward for `before`). Query `count + 1` rows with `Limit.of(count + 1)`, which
   also makes `first: 0` legal, since `Limit.of(0)` is rejected. Then
   `Window.from(page, i -> ScrollPosition.of(Map.of("published", ..., "id", ...), direction), more)`.
   The positions you hand back become the cursors.

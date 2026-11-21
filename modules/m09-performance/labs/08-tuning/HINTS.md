# Hints, lab 9.8

1. The document cache has the same shape as lab 9.7's benchmark: a `GraphQlSourceBuilderCustomizer`
   calling `configureGraphQl(b -> b.preparsedDocumentProvider(...))`, and a Caffeine cache from query
   text to `PreparsedDocumentEntry`, with a maximum size.
2. If a configuration's percentiles are measured in tens of seconds, it didn't get slow, it broke. Its
   `-gc.log` and `-app.log` say how: look for "Allocation Stall" and `OutOfMemoryError`.
3. One run per configuration can't separate differences of a few milliseconds. Before you name a
   winner, decide which differences are bigger than run-to-run noise. Then pick on those, and say what
   the others would need to win.

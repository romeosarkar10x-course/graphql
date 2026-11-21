# Lab 6.6 notes

## Error snapshot

Paste the six errors your app returns for the test's query (`tests/run.sh` shows it), one line each.

## Questions

1. Before your advice, what did the client get for all six? Compare with graphql-java's default
   handler in lab 5.6. Which default is safer, and what does the id in the message identify?
2. Spring's `ErrorType` has five values. Where did CONFLICT and TIMEOUT go, and would you rather have
   your own `ErrorClassification`?
3. `@GraphQlExceptionHandler` methods can live in a controller or in a `@ControllerAdvice`. When would you
   put one in the controller?
4. Which of the six should not have been an exception at all in a mutation, given Module 4.3?

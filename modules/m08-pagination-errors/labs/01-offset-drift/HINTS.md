# Hints, lab 8.1

1. `BookRepository` already extends `JpaRepository`, which has `findAll(Pageable)`. `PageRequest.of(page, size, sort)`
   is a `Pageable`, and the `Page<Book>` you get back has `getContent()`, `getTotalElements()`,
   `getTotalPages()` and `hasNext()`.
2. Throw your own exception for a bad `page` or `size`, and turn it into a `GraphQLError` in an
   `@GraphQlExceptionHandler` method in the controller:
   `GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(...)`. Do the checks
   before calling `PageRequest.of`, because it throws its own `IllegalArgumentException` for a negative page.
3. `addBook`: `books.save(new Book(title, Instant.now()))`. The entity's other columns have defaults and
   the id comes from the column default, which `GenerationType.IDENTITY` lets PostgreSQL fill.
   `deleteBook`: `existsById`, then `deleteById`.

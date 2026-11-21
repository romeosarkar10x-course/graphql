# Module 8 probe findings (2026-11-21)

PostgreSQL 18.7, Spring Boot 4.2.0, Spring for GraphQL 2.1.0, Spring Data JPA 4.2.0, Hibernate 7.4.15, graphql-java 26.1.

## Paging (bench.txt, deep.txt, orform.txt, scroll*.txt, conn.txt)
1. 1M rows, 20 rows at offset 900,000: OFFSET 53 ms reading 900,020 rows; row-value keyset 0.036 ms reading 20.
2. Spring Data JPA's generated keyset scroll writes `published>? or published=? and id>?`. PostgreSQL 18
   uses the index only for order, with a Filter: 900,000 rows removed at offset 900,000 (26.6 ms).
   `(published, id) > (?, ?)` is an Index Cond (0.036 ms). A redundant `published >= ?` prefix: 0.177 ms.
3. Before VACUUM, index-only scans fetch heap pages: OFFSET 900,000 took ~525 ms from Java; after
   `vacuum analyze` 25 ms. Catalog.java now vacuums.
4. Boot's default cursor (ScrollPositionCursorStrategy + base64) decodes to
   K_["java.util.Collections$UnmodifiableMap",{"id":["java.lang.Long",375],"published":1900}].
5. A custom `CursorStrategy<ScrollPosition>` bean breaks ScrollSubrange: Boot's @ConditionalOnMissingBean
   is on EncodingCursorStrategy, so two CursorStrategy beans exist; AnnotatedControllerConfigurer's
   getBean(CursorStrategy.class) throws NoUniqueBeanDefinitionException (a NoSuchBeanDefinitionException),
   which it catches and ignores: no resolver, "Parameter [0] ... was not recognized by any resolver".
   Fix: declare `EncodingCursorStrategy<ScrollPosition>` (CursorStrategy.withEncoder(...)).
6. WindowConnectionAdapter: hasPreviousPage is true only when scrolling backward and window.hasNext();
   hasNextPage only when scrolling forward. A forward page after a cursor always says hasPreviousPage false.
7. ScrollSubrange.create turns a negative count into null (the default); the Relay spec says throw.
   SubrangeMethodArgumentResolver reads first/after, and only if both are null reads last/before:
   `first: 2, before: X` silently ignores `before`.
8. Spring Data `Limit.of(0)` with a Window query: InvalidDataAccessApiUsageException "Page size must not be
   less than one" (INTERNAL_ERROR).
9. Connection types generated: `edges: [BookEdge]` (nullable list of nullable edges), `pageInfo: PageInfo!`,
   PageInfo with non-null booleans and nullable cursors.
10. When an @GraphQlExceptionHandler resolves the exception for a connection field, the response has the
   error and an empty connection in data (`books: {edges: [], pageInfo: {hasNextPage: false}}`). Unresolved
   (INTERNAL_ERROR) gives data: null.

## Errors (onerror.txt)
11. Overriding AsyncExecutionStrategy.assertNonNullFieldPrecondition to a no-op stops propagation only partly:
   `members: [User!]!` with a null item became `members: null`, `currentBook` became null after a nullable
   `rating: Float!` threw; the experimental directive keeps `members: [a, null, c]` and `rating: null`.
12. A DataFetcherExceptionHandler that throws AbortExecutionException does not halt: graphql-java wraps it as
   a DataFetchingException error with the message "halt: ...".

## Filters (filter-run.sh, count-1m.txt, mutants.txt)
13. Spring Data JPA 4: `Specification.and(null)` throws "Other specification must not be null"; use
   Specification.allOf(list). Specification.unrestricted() replaces where(null).
14. Hibernate 7.4: cb.literal("text") inside cb.function renders the string into the SQL text (quotes doubled,
   so `x') or true or ('x` stays a harmless literal). hibernate.criteria.value_handling_mode=bind does not
   change that. HibernateCriteriaBuilder.value(v) binds it as `?`.
15. 1M rows: count(*) 30 ms; genre+decade filter count 25 ms; FTS count (GIN) 9 ms; genre+decade filter
   ordered by title limit 20: 75.7 ms (walks the title index and filters).
16. Property tests vs mutants: no id tie-breaker caught on try 2 (shrunk: empty filter, TITLE DESC);
   genres [] as any: try 3; exclusive publishedTo: try 9 (shrunk to 1890..1900). The metamorphic property
   caught none of them.
17. jqwik 1.10.2 runs on Boot 4.2's JUnit 6.1.3 platform via surefire 3.6.1 with no extra config.

## Error strategy (errors.txt, depths.txt)
18. Spring for GraphQL 2.1.0 statuses: application/json always 200. application/graphql-response+json: 400 for
   every request error (no data), 200 otherwise, including data + errors and data: null + errors. Spec
   (September 2026): 422 SHOULD for validation, 294 SHOULD for data (even null) with errors.
   GraphQlHttpHandler.selectResponseStatus (protected) decides: METHOD_NOT_ALLOWED for a mutation over GET,
   UNPROCESSABLE_CONTENT for a mutation over HTTP QUERY, BAD_REQUEST when no data and graphql-response+json.
19. Spring masks unresolved fetcher exceptions as "INTERNAL_ERROR for <executionId>" (classification
   INTERNAL_ERROR) and logs "Unresolved <Exception> for executionId <id>".
20. graphql-java's default handler puts the exception message in the response:
   "Exception while fetching data (/club/owner) : users service down". A missing non-null variable is
   classified ValidationError ("Variable 'id' has an invalid value: ...").

## Games and project
21. graphql-java 26.1 checks ExecutionInput.cancel() between fetchers: a synchronous cancel stops later
   fields (cancel-probe.txt); a 900 ms fetcher under a 500 ms budget runs to the end and the abort is
   reported after it (incident 5). GraphQL.transform(b -> b.defaultDataFetcherExceptionHandler(h)) has no
   effect once the strategies exist; pass new AsyncExecutionStrategy(h).
22. Lanterna 3.1.5: DefaultVirtualTerminal.readInput is synchronized and waits holding the monitor, so a
   test thread calling getTerminalSize/getBufferCharacter blocks (thread dump). The app uses pollInput.

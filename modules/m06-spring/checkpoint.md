# Checkpoint 6 quiz

1. With platform threads, why does Spring MVC top out near 2,000 req/s for a field that blocks 100 ms?
2. What does `ArgumentValue<String>` give you that `String` does not?
3. The inspection reports `int first should be NULLABLE` for `first: Int = 10`. Explain, and give the fix.
4. How does a header value reach `@ContextValue` in a field three levels deep?
5. What does Spring send to the client for an exception you did not map, and where are the details?
6. Why did the DGS app need graphql-java pinned to 25.0?
7. Why does Quarkus emit `id: String` until you annotate, and what makes `@Source` batched?
8. Name the case where native image is clearly worth its costs, and two of those costs.

<details><summary>Answers</summary>

1. Tomcat's 200 threads each hold a request for 0.1 s: 200 / 0.1 = 2,000. Virtual threads lift it.
2. The difference between an omitted argument (`isOmitted()`) and an explicit `null`.
3. A client can send `first: null` explicitly; a primitive cannot hold it. Use `@Nullable Integer` with a
   fallback.
4. A `WebGraphQlInterceptor` adds it to the ExecutionInput's `GraphQLContext`, which every fetcher of the
   request sees.
5. `INTERNAL_ERROR for <execution id>`; the exception is logged with that id.
6. DGS 12.1.1 was built against 25.0; Boot 4.2 manages 26.1, which removed a class DGS uses.
7. Code-first cannot know a String is an id or non-null without `@Id` / `@NonNull`. A `@Source List<...>`
   parameter.
8. Scale-to-zero or short-lived processes; costs: slow, memory-hungry builds, reflection metadata, no JIT
   peak, harder debugging and agents.
</details>

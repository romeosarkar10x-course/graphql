# DESIGN.md — the tiny starter

1. **API.** `TinyGraphQL.builder().schema("/shelfie.graphqls").scan("dev.shelfie.app").context(...).start(4000)`
   plus `@GraphQLController` classes with `@Field(type = "Club") owner(Data.Club c)`. Convention: field name
   = method name, argument name = parameter name (`-parameters`), first parameter = parent for non-root
   types. Explicit: the type (a method name cannot say which type it belongs to, and `reviews` exists on
   two), and `@Context` keys (context values have no natural name). Unannotated record components resolve
   by graphql-java's `PropertyDataFetcher`, as before.
2. **Fail fast.** Startup catches a missing schema resource, a controller without a no-argument
   constructor, two methods for one coordinate, and a method naming a field that is not in the schema
   (the test's `memberCountt`). Still at request time: a schema field nobody resolves (it silently uses
   `PropertyDataFetcher`), wrong parameter types (a `ClassCastException` per call), and nullability lies.
   Spring's inspection (6.3) also reports unmapped fields and nullness; adding the unmapped-field check
   means walking every object type and asking whether a method or a matching record component exists.
3. From `target/classes` the package is a directory; from the jar it is a `jar:` URL whose entries must be
   listed through `JarURLConnection`, and nested jars (Spring Boot's fat jar layout) would need yet another
   case. Spring scans with ASM-based metadata readers that do not even load the classes; in a native image
   class-path scanning is impossible at runtime (there is no class path), so the scan must move to build
   time, an annotation processor generating the registry, which is what Micronaut and Quarkus do.
4. Measured with lab 6.9's Bench (`_workbench/2026-11-21-m05-probe/tiny-vs-raw.txt`, against the
   bake-off's raw row): the tiny starter does about 11,400 req/s with p95 1.28 ms, the Module 5 server
   about 13,400 req/s with p95 1.08 ms. Roughly 15% slower, which is Javalin's Jetty stack plus
   `Method.invoke` per field; per request that is well under 0.2 ms, against everything else a real
   fetcher does (a database call is milliseconds). It does not matter for Shelfie; it would for a query
   resolving hundreds of thousands of fields, where `MethodHandle`s or generated code would help.
5. (1) Unmapped-field and parameter-type checks at startup; (2) `CompletableFuture` and DataLoader support
   (Module 8) so fetchers can batch and run async; (3) a pluggable exception handler (5.6) and a document
   cache (5.8). Packaging as a Javalin plugin comes after those.

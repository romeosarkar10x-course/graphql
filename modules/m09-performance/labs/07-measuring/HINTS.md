# Hints, lab 9.7

1. JMH: put the setup in a `@Setup` method of a `@State(Scope.Benchmark)` class, and return the result
   of each `@Benchmark` method so the JIT can't remove the work. `SchemaGenerator().makeExecutableSchema(...)`
   with `RuntimeWiring.newRuntimeWiring().build()` is enough: every field returns null.
2. Gatling Java DSL: `scenario("dashboard").exec(http("dashboard").post("").body(StringBody(json)).check(status().is(200), jsonPath("$.data.clubs").exists()))`,
   then in the constructor block `setUp(scn.injectOpen(...)).protocols(http.baseUrl(URL)).assertions(global().failedRequests().count().is(0L))`.
3. If Micrometer's p99 is far above Gatling's, look at what each one measured and over which period.
   Micrometer's percentiles come from a histogram over a sliding window in the app, which still holds
   the warm-up. Gatling's are only the measured phase, seen from the client.

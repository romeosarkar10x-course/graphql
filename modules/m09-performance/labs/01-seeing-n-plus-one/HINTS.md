# Hints, lab 9.1

1. `ProxyDataSourceBuilder.create(dataSource).listener(...).build()` returns a `DataSource` that runs
   your listener around every statement. A `BeanPostProcessor` lets you return it in place of the
   `DataSource` bean Spring Boot made: check `bean instanceof DataSource` in
   `postProcessAfterInitialization`.
2. `QueryExecutionListener.afterQuery(ExecutionInfo, List<QueryInfo>)` gets one `QueryInfo` per statement
   (a batch can hold several). Keep the total in a static `AtomicLong`; the interceptor only needs the
   difference between two readings.
3. In the interceptor, read the counter, then `chain.next(request).map(response -> response.transform(b -> b.extensions(...)))`.
   The `map` runs after execution has finished, loaders included, so the second reading is complete.

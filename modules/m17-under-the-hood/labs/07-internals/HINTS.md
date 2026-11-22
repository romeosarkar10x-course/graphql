# Hints: 17.7 graphql-java internals

1. Start from the entry point, `graphql.GraphQL.execute`, and follow the calls: parsing and validation happen in
   `ParseAndValidate`, execution in `Execution`, which picks an `ExecutionStrategy`. An IDE's "go to definition"
   on the clone is faster than grep.
2. Gradle's `--tests` takes a class name pattern; a Spock class is a test class like any other. The first run
   downloads Gradle and the dependencies, so time the second.
3. Read the `version` and `signing` blocks of `build.gradle` before you publish: they decide which version lands in
   `~/.m2` and whether signing keys are needed. The pom that uses it needs that exact version.

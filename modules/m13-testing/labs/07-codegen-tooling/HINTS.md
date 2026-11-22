# Hints: m13/07

1. Lab 6.7's pom has the plugin and the `build-helper-maven-plugin` execution that adds
   `target/generated-sources` to the compile; point `schemaPaths` at `src/main/resources/graphql` and pick a
   `packageName`. Run `mvn generate-sources` and read `DgsConstants.java` before you touch a controller.
2. A rename only breaks the build if the code names the field through a generated symbol. A mapping that relies
   on the method name (`@SchemaMapping public Double averageRating(Book book)`) or a string still compiles after
   the schema changes; `field = BOOK.AverageRating` does not. Arguments too: `@Argument(BOOK.REVIEWS_INPUT_ARGUMENT.Last)`.
3. SpectaQL wants `introspection.schemaFile` (no server needed) and a `targetDir`; `oneFile: true` gives a
   single page. graphql-config reads `schema:` and `documents:` globs relative to the config file.

# Hints, lab 6.7

1. The plugin's `generate` goal takes `schemaPaths` (a list of files or directories) and `packageName`; it
   writes to `target/generated-sources`, which Maven does not compile until `build-helper-maven-plugin`'s
   `add-source` goal adds it (Boot's parent manages that plugin's version).
2. Run `mvn -q generate-sources` and look at the generated `types/Club.java`: every type has a builder,
   and `DgsConstants.CLUB.CurrentBook` is the string `"currentBook"`.
3. The generated `Club` has no `ownerId`: either build the nested `owner` eagerly in the `club` fetcher, or
   look the club up again from the source's id in a `@DgsData` fetcher.

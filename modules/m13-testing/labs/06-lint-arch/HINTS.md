# Hints: m13/06

1. Run each tool on the clean project before you write a single rule of your own, and read what it reports.
   graphql-eslint's schema rules that look at the whole schema need it: give the parser
   `parserOptions: { graphQLConfig: { schema: '...' } }`. A rule that fires on every line of a correct schema is
   a style you have not chosen yet, not a bug.
2. `layeredArchitecture()` says which layers may meet. Controllers return the store's records, so graph may
   use store, and then a controller calling a repository breaks no layer. Ban it directly: no class in
   `..graph..` may depend on a class annotated `@Repository`.
3. "Services know nothing of GraphQL" is a dependency rule on packages: `graphql..` and
   `org.springframework.graphql..`. For Spotless, steps such as `removeUnusedImports`, `trimTrailingWhitespace`
   and `indent` check hygiene without reformatting the whole code base; a full formatter needs one
   `spotless:apply` first.

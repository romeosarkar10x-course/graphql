# Mini-project 14.8: A headless CMS in a weekend

Lecture: [14.7 Dynamic and multiple schemas](../../lectures/14.7-dynamic-schemas.md). Grading: auto. Needs
PostgreSQL (`bin/course ref up`).

Content types are rows in tables; each becomes a GraphQL type with its own queries and mutations, at run time.
The fixed part of the API is in the starter (`graphql/cms.graphqls`): `contentTypes`, `defineContentType`,
`removeContentType`. For a content type `Recipe` with plural `Recipes`, generate:

```graphql
type Recipe { id: ID! createdAt: String! title: String! minutes: Int author: Author ... }   # one field per definition
input RecipeInput { title: String! minutes: Int author: ID ... }                            # required stays required
input RecipePatch { title: String minutes: Int author: ID ... }                             # everything optional
extend type Query { recipe(id: ID!): Recipe  recipes(first: Int = 20): [Recipe!]! }        # oldest first
extend type Mutation {
  createRecipe(input: RecipeInput!): Recipe            # null + BAD_REQUEST when a reference points nowhere
  updateRecipe(id: ID!, patch: RecipePatch!): Recipe   # null for an unknown id; a required field cannot become null
  deleteRecipe(id: ID!): ID                            # refused while another entry references it
}
```

Field kinds: `STRING`, `INT`, `FLOAT`, `BOOLEAN`, `REFERENCE` (with `ref`, the name of a content type; the field's
type is that type, its input is an `ID`). Descriptions become schema descriptions.

Definitions must keep the schema valid and the entries meaningful: PascalCase names the API does not already use
(`Query`, `ContentType`, `...Input`), a plural unlike any other type's, camelCase field names other than `id` and
`createdAt`, references to types that exist. Redefining a type with entries may add optional fields, but not a
required one, and may not change a field's kind. A type another references cannot be removed. Everything survives
a restart.

```bash
bin/course start m14/08
cd workspace/m14-patterns/08-project-cms
bin/course shell -c 'mvn -q package && java -jar target/shelfie-cms.jar'
bin/course test m14/08     # resets m14_08, runs cms.hurl, restarts your app, runs cms-restart.hurl
```

## Group variant: content-model race

Pairs get the same brief from another player, a site to model in five minutes of talking ("a cookbook with
authors, recipes and ingredients", "a book club with meetups and attendees"). Each pair defines it on their own
CMS, then swaps endpoint URLs (`COURSE_PUBLISH=4000 bin/course shell ...`) with another pair, who must build a
page's query from introspection alone, with no help. One point per pair whose query worked first time; one point
for every rule of yours their definitions broke (send them definitions your CMS refuses and theirs accepts, and
argue it out with the README as the referee).

## Done when

`bin/course test m14/08` passes.

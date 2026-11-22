# Hints: m12/04

1. The typesafe client is an interface annotated `@GraphQLClientApi(configKey = "shelfie")`; a method
   annotated `@Query` named `club` becomes the root field, and you `@Inject` the interface. The dynamic client
   is `@Inject @GraphQLClient("shelfie") DynamicGraphQLClient`.
2. If Shelfie answers with a VariableTypeMismatch, compare the variable type the client declared with the
   argument's type in the schema. For `reviews(last: 2)` on a nested field, SmallRye has an annotation that
   puts a method parameter on a field deeper in the selection.
3. Dynamic: `document(operation(QUERY, "name", vars(var("id", nonNull(GQL_ID))), field("club", args(arg("id",
   var...)), field("name"), ...)))`, then `executeSync(doc, Map.of("id", id))` and `getObject(Club.class, "club")`.

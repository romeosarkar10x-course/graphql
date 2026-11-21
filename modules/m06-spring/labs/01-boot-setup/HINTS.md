# Hints, lab 6.1

1. A `@SchemaMapping` method's name is the field name and its first parameter's type tells Spring the
   parent type; when two types share a field name (`User.reviews`, `Book.reviews`), say
   `@SchemaMapping(typeName = "Book")`.
2. Arguments arrive with schema defaults applied: `@Argument int first` works for `members(first: Int = 10)`
   (lab 6.3 will tell you why `int` is still not quite right). Input objects bind to records:
   `@Argument CreateReviewInput input`.
3. The two properties are `spring.graphql.graphiql.enabled` and `spring.threads.virtual.enabled`.

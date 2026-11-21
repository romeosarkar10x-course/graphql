# Hints, lab 6.8

1. A public method on a `@GraphQLApi` bean that is not `@Query` or `@Mutation` and takes a `@Source X`
   parameter becomes a field of type `X`. Take `@Source List<X>` and return a `List` in the same order,
   and SmallRye calls it once with all the parents.
2. `org.eclipse.microprofile.graphql.Id` turns a String into `ID`; `@NonNull` adds the `!`. On a generic
   element or a nested type, a type annotation sits next to the simple name: `List<Types.@NonNull Club>`.
3. Only public fields and getters become schema fields: package-private `ownerId` stays internal.

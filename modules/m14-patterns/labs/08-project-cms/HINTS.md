# Hints: m14/08

1. Lab 14.7's dynamic `GraphQlSource` is the engine. Generating SDL text for each content type and parsing it with
   the fixed part (`SchemaParser` plus `SchemaGenerator` with a `RuntimeWiring`) is easier to read and debug than
   building every type in code: print the SDL when something fails.
2. Escape what users write: a description goes into SDL as a string, and JSON string escaping is valid GraphQL
   string escaping. Mutation names come from type names, so validate names before you generate anything, and
   rebuild after every define or remove.
3. GraphQL's input types already refuse a missing required field and a wrong type; what they cannot know is
   whether a referenced id exists, which you check before insert, and what existing entries hold, which decides
   whether a redefinition is safe. A non-null mutation result turns one refused alias into `data: null` for the
   whole response.

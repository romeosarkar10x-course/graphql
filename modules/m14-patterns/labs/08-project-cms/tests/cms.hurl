# Mini-project 14.8, part 1: content types become an API.

# H01 a content type
POST {{url}}
Content-Type: application/json
{"query": "mutation { defineContentType(input: {name: \"Author\", plural: \"Authors\", description: \"Someone who writes recipes.\", fields: [{name: \"name\", kind: STRING, required: true, description: \"As printed on the book.\"}, {name: \"born\", kind: INT}]}) { contentType { name plural entryCount fields { name kind required } } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.defineContentType.userErrors" count == 0
jsonpath "$.data.defineContentType.contentType.name" == "Author"
jsonpath "$.data.defineContentType.contentType.fields" count == 2
jsonpath "$.data.defineContentType.contentType.fields[0].required" == true

# H02 a second one, referencing the first
POST {{url}}
Content-Type: application/json
{"query": "mutation { defineContentType(input: {name: \"Recipe\", plural: \"Recipes\", fields: [{name: \"title\", kind: STRING, required: true}, {name: \"minutes\", kind: INT}, {name: \"vegetarian\", kind: BOOLEAN}, {name: \"rating\", kind: FLOAT}, {name: \"author\", kind: REFERENCE, ref: \"Author\"}]}) { contentType { name } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.defineContentType.userErrors" count == 0

# H03 they are GraphQL types now, with queries, mutations and inputs (one __type and one fields per request:
# graphql-java refuses repeated introspection fields as bad-faith introspection)
POST {{url}}
Content-Type: application/json
{"query": "{ __schema { queryType { fields { name } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__schema.queryType.fields[*].name" contains "recipe"
jsonpath "$.data.__schema.queryType.fields[*].name" contains "recipes"
jsonpath "$.data.__schema.queryType.fields[*].name" contains "authors"

POST {{url}}
Content-Type: application/json
{"query": "{ __schema { mutationType { fields { name } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__schema.mutationType.fields[*].name" contains "createRecipe"
jsonpath "$.data.__schema.mutationType.fields[*].name" contains "updateAuthor"
jsonpath "$.data.__schema.mutationType.fields[*].name" contains "deleteRecipe"

POST {{url}}
Content-Type: application/json
{"query": "{ __type(name: \"RecipeInput\") { inputFields { name type { kind name ofType { name } } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type.inputFields[0].name" == "title"
jsonpath "$.data.__type.inputFields[0].type.kind" == "NON_NULL"
jsonpath "$.data.__type.inputFields[4].type.name" == "ID"

POST {{url}}
Content-Type: application/json
{"query": "{ __type(name: \"Recipe\") { fields { name type { name kind } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type.fields[0].name" == "id"
jsonpath "$.data.__type.fields[6].name" == "author"
jsonpath "$.data.__type.fields[6].type.name" == "Author"

POST {{url}}
Content-Type: application/json
{"query": "{ __type(name: \"Author\") { description fields { name description } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type.description" == "Someone who writes recipes."
jsonpath "$.data.__type.fields[2].description" == "As printed on the book."

# H04 entries
POST {{url}}
Content-Type: application/json
{"query": "mutation { createAuthor(input: {name: \"Yotam Ottolenghi\", born: 1968}) { id name born createdAt } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createAuthor.name" == "Yotam Ottolenghi"
jsonpath "$.data.createAuthor.createdAt" matches /^\d{4}-\d\d-\d\dT/
[Captures]
author: jsonpath "$.data.createAuthor.id"

POST {{url}}
Content-Type: application/json
{"query": "mutation { a: createRecipe(input: {title: \"Shakshuka\", minutes: 30, vegetarian: true, rating: 4.5, author: \"{{author}}\"}) { id } b: createRecipe(input: {title: \"Toast\"}) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
[Captures]
recipe: jsonpath "$.data.a.id"

# H05 reading them back, references followed
POST {{url}}
Content-Type: application/json
{"query": "{ recipe(id: \"{{recipe}}\") { title minutes vegetarian rating author { name born } } recipes { title author { name } } one: recipes(first: 1) { title } nope: recipe(id: \"recipe-0\") { title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.recipe.title" == "Shakshuka"
jsonpath "$.data.recipe.minutes" == 30
jsonpath "$.data.recipe.vegetarian" == true
jsonpath "$.data.recipe.rating" == 4.5
jsonpath "$.data.recipe.author.name" == "Yotam Ottolenghi"
jsonpath "$.data.recipes" count == 2
jsonpath "$.data.recipes[1].title" == "Toast"
jsonpath "$.data.recipes[1].author" == null
jsonpath "$.data.one" count == 1
jsonpath "$.data.nope" == null

# H06 GraphQL's own validation does the typing: a missing required field, a wrong type
POST {{url}}
Content-Type: application/json
{"query": "mutation { createRecipe(input: {minutes: 10}) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "ValidationError"

POST {{url}}
Content-Type: application/json
{"query": "mutation { createRecipe(input: {title: \"Soup\", minutes: \"ten\"}) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "ValidationError"

# H07 a reference to nothing is refused, alone: the other field in the request succeeds
POST {{url}}
Content-Type: application/json
{"query": "mutation { bad: createRecipe(input: {title: \"Ghost stew\", author: \"author-999\"}) { id } good: createAuthor(input: {name: \"Meera Sodha\"}) { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.bad" == null
jsonpath "$.errors[0].path[0]" == "bad"
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"
jsonpath "$.data.good.name" == "Meera Sodha"

# H08 patches change what they name; required stays required; an unknown id is null
POST {{url}}
Content-Type: application/json
{"query": "mutation { a: updateRecipe(id: \"{{recipe}}\", patch: {minutes: 25}) { title minutes } b: updateRecipe(id: \"{{recipe}}\", patch: {title: null}) { title } c: updateRecipe(id: \"recipe-0\", patch: {minutes: 1}) { title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.a.minutes" == 25
jsonpath "$.data.a.title" == "Shakshuka"
jsonpath "$.data.b" == null
jsonpath "$.errors[0].path[0]" == "b"
jsonpath "$.data.c" == null

# H09 an entry something points at cannot be deleted
POST {{url}}
Content-Type: application/json
{"query": "mutation { deleteAuthor(id: \"{{author}}\") }"}
HTTP 200
[Asserts]
jsonpath "$.data.deleteAuthor" == null
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"

# H10 redefining: a new optional field is fine; existing entries have it as null
POST {{url}}
Content-Type: application/json
{"query": "mutation { defineContentType(input: {name: \"Recipe\", plural: \"Recipes\", fields: [{name: \"title\", kind: STRING, required: true}, {name: \"minutes\", kind: INT}, {name: \"vegetarian\", kind: BOOLEAN}, {name: \"rating\", kind: FLOAT}, {name: \"author\", kind: REFERENCE, ref: \"Author\"}, {name: \"servings\", kind: INT}]}) { userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.defineContentType.userErrors" count == 0

POST {{url}}
Content-Type: application/json
{"query": "{ recipe(id: \"{{recipe}}\") { title servings } }"}
HTTP 200
[Asserts]
jsonpath "$.data.recipe.title" == "Shakshuka"
jsonpath "$.data.recipe.servings" == null

# H11 but not what the entries cannot meet: a new required field, a field changing kind
POST {{url}}
Content-Type: application/json
{"query": "mutation { a: defineContentType(input: {name: \"Recipe\", plural: \"Recipes\", fields: [{name: \"title\", kind: STRING, required: true}, {name: \"cuisine\", kind: STRING, required: true}]}) { contentType { name } userErrors { field message } } b: defineContentType(input: {name: \"Recipe\", plural: \"Recipes\", fields: [{name: \"title\", kind: STRING, required: true}, {name: \"minutes\", kind: STRING}]}) { userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.a.contentType" == null
jsonpath "$.data.a.userErrors" count == 1
jsonpath "$.data.b.userErrors" count == 1

# H12 definitions that would break the schema
POST {{url}}
Content-Type: application/json
{"query": "mutation { lower: defineContentType(input: {name: \"book\", plural: \"Books\", fields: []}) { userErrors { message } } reserved: defineContentType(input: {name: \"Query\", plural: \"Queries\", fields: []}) { userErrors { message } } taken: defineContentType(input: {name: \"Book\", plural: \"Authors\", fields: []}) { userErrors { message } } id: defineContentType(input: {name: \"Book\", plural: \"Books\", fields: [{name: \"id\", kind: STRING}]}) { userErrors { message } } ref: defineContentType(input: {name: \"Book\", plural: \"Books\", fields: [{name: \"shelf\", kind: REFERENCE, ref: \"Shelf\"}]}) { userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.lower.userErrors" count >= 1
jsonpath "$.data.reserved.userErrors" count >= 1
jsonpath "$.data.taken.userErrors" count >= 1
jsonpath "$.data.id.userErrors" count >= 1
jsonpath "$.data.ref.userErrors" count >= 1

POST {{url}}
Content-Type: application/json
{"query": "{ __type(name: \"Book\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type" == null

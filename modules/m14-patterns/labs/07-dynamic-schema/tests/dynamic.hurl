# Lab 14.7, part 1: two endpoints in one JVM; the admin one changes the public one.

# Y01 the public schema comes from the metadata: id and title, then author and publishedYear
POST {{url}}
Content-Type: application/json
{"query": "{ __type(name: \"Book\") { fields { name description } } books { id title author publishedYear } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type.fields" count == 4
jsonpath "$.data.__type.fields[2].name" == "author"
jsonpath "$.data.__type.fields[3].name" == "publishedYear"
jsonpath "$.data.__type.fields[3].description" == "Year of first publication."
jsonpath "$.data.books" count == 3
jsonpath "$.data.books[0].publishedYear" == 1969

# Y02 two schemas: the public one has no mutations
POST {{url}}
Content-Type: application/json
{"query": "{ __schema { mutationType { name } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__schema.mutationType" == null

# Y03 and the admin one has no books
POST {{admin}}
X-Admin-Key: course-admin-key
Content-Type: application/json
{"query": "{ books { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "ValidationError"

# Y04 the admin endpoint wants its key
POST {{admin}}
Content-Type: application/json
{"query": "{ fields { name } }"}
HTTP 401

# Y05 add a field: a column, a metadata row, and the public schema has it at once
POST {{admin}}
X-Admin-Key: course-admin-key
Content-Type: application/json
{"query": "mutation { addField(name: \"pageCount\", type: INT, description: \"Pages in the first edition.\") { field { name type column } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.addField.userErrors" count == 0
jsonpath "$.data.addField.field.name" == "pageCount"
jsonpath "$.data.addField.field.type" == "INT"

POST {{url}}
Content-Type: application/json
{"query": "{ __type(name: \"Book\") { fields { name description type { name } } } book(id: \"b1\") { title pageCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type.fields[4].name" == "pageCount"
jsonpath "$.data.__type.fields[4].description" == "Pages in the first edition."
jsonpath "$.data.__type.fields[4].type.name" == "Int"
jsonpath "$.data.book.pageCount" == null

# Y06 names GraphQL refuses, and names already taken, are user errors
POST {{admin}}
X-Admin-Key: course-admin-key
Content-Type: application/json
{"query": "mutation { a: addField(name: \"page-count\", type: INT) { field { name } userErrors { message } } b: addField(name: \"__secret\", type: STRING) { userErrors { message } } c: addField(name: \"title\", type: STRING) { userErrors { message } } d: addField(name: \"pageCount\", type: INT) { userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.a.field" == null
jsonpath "$.data.a.userErrors" count == 1
jsonpath "$.data.b.userErrors" count == 1
jsonpath "$.data.c.userErrors" count == 1
jsonpath "$.data.d.userErrors" count == 1

# Y07 the admin endpoint lists the metadata
POST {{admin}}
X-Admin-Key: course-admin-key
Content-Type: application/json
{"query": "{ fields { name type column } }"}
HTTP 200
[Asserts]
jsonpath "$.data.fields" count == 3
jsonpath "$.data.fields[2].name" == "pageCount"

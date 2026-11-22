# Mini-project 14.8, part 2: after a restart, and removing types.

# R01 the types and the entries survived the restart
POST {{url}}
Content-Type: application/json
{"query": "{ contentTypes { name entryCount fields { name } } recipes { id title servings author { name } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.contentTypes" count == 2
jsonpath "$.data.contentTypes[0].name" == "Author"
jsonpath "$.data.contentTypes[0].entryCount" == 2
jsonpath "$.data.contentTypes[1].fields" count == 6
jsonpath "$.data.recipes[0].title" == "Shakshuka"
jsonpath "$.data.recipes[0].author.name" == "Yotam Ottolenghi"
[Captures]
r1: jsonpath "$.data.recipes[0].id"
r2: jsonpath "$.data.recipes[1].id"

# R02 a type others reference cannot go
POST {{url}}
Content-Type: application/json
{"query": "mutation { removeContentType(name: \"Author\") { removed userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.removeContentType.removed" == null
jsonpath "$.data.removeContentType.userErrors" count == 1

# R03 entries deleted, then the type, then its API is gone
POST {{url}}
Content-Type: application/json
{"query": "mutation { a: deleteRecipe(id: \"{{r1}}\") b: deleteRecipe(id: \"{{r2}}\") again: deleteRecipe(id: \"{{r1}}\") }"}
HTTP 200
[Asserts]
jsonpath "$.data.a" == "{{r1}}"
jsonpath "$.data.again" == null

POST {{url}}
Content-Type: application/json
{"query": "mutation { removeContentType(name: \"Recipe\") { removed userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.removeContentType.removed" == "Recipe"

POST {{url}}
Content-Type: application/json
{"query": "{ recipes { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "ValidationError"

# R04 and now Author can go too
POST {{url}}
Content-Type: application/json
{"query": "mutation { removeContentType(name: \"Author\") { removed } }"}
HTTP 200
[Asserts]
jsonpath "$.data.removeContentType.removed" == "Author"

POST {{url}}
Content-Type: application/json
{"query": "{ contentTypes { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.contentTypes" count == 0

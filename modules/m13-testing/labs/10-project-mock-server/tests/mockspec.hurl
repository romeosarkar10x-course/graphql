# Mini-project 13.10: MockSpec (RFC 0, strawman) read by the mock server. Run against
#   java -jar target/mock-server.jar shelfie.graphqls --mocks <a copy of tests/mocks>
# Mock files are named after the operation: BookCard.json answers "query BookCard ... @mock".

# M01 @mock answers from the operation's file, "__default__" entry
POST {{url}}
Content-Type: application/json
{"query": "query BookCard @mock { book(id: \"b2\") { title averageRating } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.title" == "Piranesi"
jsonpath "$.data.book.averageRating" == 4.5
jsonpath "$.errors" not exists

# M02 @mock(name:) picks a named mock; __description__ is for people, not for the response
POST {{url}}
Content-Type: application/json
{"query": "query BookCard @mock(name: \"unrated\") { book(id: \"b5\") { title averageRating } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.title" == "Good Omens"
jsonpath "$.data.book.averageRating" == null
jsonpath "$.__description__" not exists

# M03 a named mock can be an error state: null data at the root and an error with a path
POST {{url}}
Content-Type: application/json
{"query": "query BookCard @mock(name: \"fetch_error\") { book(id: \"b2\") { title averageRating } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book" == null
jsonpath "$.errors[0].message" == "internal server error"
jsonpath "$.errors[0].path[0]" == "book"

# M04 an unknown mock name is an error that lists the names there are
POST {{url}}
Content-Type: application/json
{"query": "query BookCard @mock(name: \"nope\") { book(id: \"b2\") { title } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" contains "nope"
jsonpath "$.errors[0].message" contains "unrated"
jsonpath "$.errors[0].message" contains "fetch_error"

# M05 a mock that no longer fits the operation is refused, not served (title is non-null and missing)
POST {{url}}
Content-Type: application/json
{"query": "query Broken @mock { book(id: \"b2\") { title averageRating } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" contains "does not match"
jsonpath "$.data.book" not exists

# M06 no file yet: the server generates one from fake data and answers with it
POST {{url}}
Content-Type: application/json
{"query": "query NewScreen @mock { club(id: \"c1\") { id name memberCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.id" == "c1"
jsonpath "$.data.club.name" isString
jsonpath "$.data.club.memberCount" isInteger
[Captures]
first_name: jsonpath "$.data.club.name"

# M07 and the same request again is answered from the file it wrote
POST {{url}}
Content-Type: application/json
{"query": "query NewScreen @mock { club(id: \"c1\") { id name memberCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "{{first_name}}"

# M08 without @mock the operation gets fake data, not the file
POST {{url}}
Content-Type: application/json
{"query": "query BookCard { book(id: \"b2\") { title averageRating } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.title" isString
jsonpath "$.data.book.title" != "Piranesi"

# M09 MockSpec needs a name: an anonymous operation with @mock is an error
POST {{url}}
Content-Type: application/json
{"query": "query @mock { hello }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" contains "named operation"

# M10 introspection works, and the schema now declares @mock
POST {{url}}
Content-Type: application/json
{"query": "{ __schema { queryType { name } directives { name } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__schema.queryType.name" == "Query"
jsonpath "$.data.__schema.directives[*].name" includes "mock"

# M11 only POST
GET {{url}}
HTTP 405

# M12 a body that is not JSON is a GraphQL error, not a crash
POST {{url}}
Content-Type: application/json
`{"query": `
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" exists

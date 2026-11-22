# Lab 14.1: the grader's own schema with the directives moved to other fields. Behaviour must follow the SDL.

# X01 @uppercase moved to title
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b2\") { title genre } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.title" == "PIRANESI"
jsonpath "$.data.book.genre" == "fantasy"

# X02 @mask(keep: 1) moved to handle, email shown whole
POST {{url}}
Content-Type: application/json
{"query": "{ user(id: \"u1\") { handle email } }"}
HTTP 200
[Asserts]
jsonpath "$.data.user.handle" == "**a"
jsonpath "$.data.user.email" == "ada.lovelace@example.com"

# X03 @currency(code: "EUR") on priceUsd: the code comes from the SDL, not from the field name
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b4\") { priceUsd } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.priceUsd" == "EUR 14.25"

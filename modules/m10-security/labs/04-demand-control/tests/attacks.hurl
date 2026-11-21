# Lab 10.4: lab 10.3's attacks, now refused. Each must come back with an error and no data.

# X01 Depth 12: over the depth limit of 10
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b1\") { reviews(first: 1) { book { reviews(first: 1) { book { reviews(first: 1) { book { reviews(first: 1) { book { reviews(first: 1) { book { reviews(first: 1) { body } } } } } } } } } } } } }"}
HTTP *
[Asserts]
jsonpath "$.errors[0].message" contains "depth"
jsonpath "$.data" not exists

# X02 600 fields through aliases: over the field limit of 500
POST {{url}}
Content-Type: application/json
file,aliases-600.json;
HTTP *
[Asserts]
jsonpath "$.errors[0].message" contains "fields"
jsonpath "$.data" not exists

# X03 Fan-out at depth 7: inside the depth limit, but its cost is in the millions
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 12) { members(first: 10) { clubs(first: 12) { members(first: 10) { clubs(first: 12) { members(first: 10) { clubs(first: 12) { name } } } } } } } }"}
HTTP *
[Asserts]
jsonpath "$.errors[0].message" contains "complexity"
jsonpath "$.data" not exists

# X04 The same fan-out hidden in fragments
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 12) { ...M } } fragment M on Club { members(first: 10) { ...C } } fragment C on Reader { clubs(first: 12) { members(first: 10) { clubs(first: 12) { members(first: 10) { name } } } } }"}
HTTP *
[Asserts]
jsonpath "$.errors[0].message" contains "complexity"

# X05 One huge page: first is capped at 50
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 1000) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" contains "first must be"
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"

# X06 A negative page size
POST {{url}}
Content-Type: application/json
{"query": "{ readers(first: -1) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" count >= 1

# X07 Cheap in cost but slow (10 recommender calls, 3 s): cancelled after 1 s
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 10) { recommendations { title } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" contains "cancel"
duration < 2500

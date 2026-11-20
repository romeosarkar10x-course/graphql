# Lab 0.1: your first conformance test.
POST {{ref}}/graphql
Content-Type: application/json
{"query": "{ hello }"}

HTTP 200
[Asserts]
header "Content-Type" contains "application/json"
jsonpath "$.errors" not exists
jsonpath "$.data.hello" == "Hello from Shelfie!"

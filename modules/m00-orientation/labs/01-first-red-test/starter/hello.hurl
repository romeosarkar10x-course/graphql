# Lab 0.1: your first conformance test.
# Hurl sends the request below, then checks every assertion under [Asserts].
# {{ref}} is the reference server; bin/course hurl fills it in.

POST {{ref}}/graphql
Content-Type: application/json
{"query": "{ hello }"}

HTTP 200
[Asserts]
jsonpath "$.data.hello" == "Hello, World!"

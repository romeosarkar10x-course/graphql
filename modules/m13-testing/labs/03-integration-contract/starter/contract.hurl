# Lab 13.3: a black-box contract for Shelfie's reads and createReview, for both the Module 5 and the
# Module 13 servers. Title every entry "# Cnn what it checks". Run it against a freshly started server.
#   hurl --test --variable url=http://localhost:4000/graphql contract.hurl

# C01 the server answers
POST {{url}}
Content-Type: application/json
{"query": "{ hello }"}
HTTP 200
[Asserts]
jsonpath "$.data.hello" exists

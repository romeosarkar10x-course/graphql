# My forbidden paths. One entry per attempt, titled "# F01 ...", "# F02 ...": at least 10.
# Variables: {{url}}, and a token for each user: {{ada}} {{ben}} {{max}} {{olu}} {{root}}.
# Example (passes once club(id:) checks membership):

# F01 ben asks for private club c1 directly: null
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club" == null

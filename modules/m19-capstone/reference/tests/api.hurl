# Toolshed's conformance suite, through the router, with persisted document ids only.
# Variables: api (the router's URL), token_member (ada), token_other (olu), token_admin (root), and the ids from
# tests/ids.env (scripts/persist.sh). The course checker passes them all; by hand:
#   hurl --test --variables-file tests/ids.env --variable api=http://localhost:4000/graphql \
#        --variable token_member=$(bin/course token ada --aud toolshed-api) ... tests/api.hurl
# The stack must start from the seed (each subgraph reloads it at start).

# 1. Anyone can browse: the first page of the catalogue, by name.
POST {{api}}
{"documentId": "{{ToolPage}}", "variables": {"first": 3}}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.tools.totalCount" == 30
jsonpath "$.data.tools.edges" count == 3
jsonpath "$.data.tools.edges[0].node.name" == "Angle grinder"
jsonpath "$.data.tools.edges[0].node.specs.__typename" == "PowerSpecs"
jsonpath "$.data.tools.edges[0].node.available" == true
jsonpath "$.data.tools.pageInfo.hasNextPage" == true
[Captures]
cursor: jsonpath "$.data.tools.pageInfo.endCursor"

# 2. The next page starts after the cursor: no overlap.
POST {{api}}
{"documentId": "{{ToolPage}}", "variables": {"first": 3, "after": "{{cursor}}"}}
HTTP 200
[Asserts]
jsonpath "$.data.tools.edges[0].node.name" == "Claw hammer"
jsonpath "$.data.tools.edges" count == 3

# 3. Filter and sort: hand tools in the garden, newest first.
POST {{api}}
{"documentId": "{{ToolPage}}", "variables": {"first": 10, "filter": {"categoryId": "cat-garden", "powered": false}, "sort": "NEWEST"}}
HTTP 200
[Asserts]
jsonpath "$.data.tools.totalCount" == 3
jsonpath "$.data.tools.edges[0].node.name" == "Pruning shears"
jsonpath "$.data.tools.edges[0].node.specs.__typename" == "HandSpecs"
jsonpath "$.data.tools.pageInfo.hasNextPage" == false

# 4. A page larger than 50 is a client mistake, with the argument named.
POST {{api}}
{"documentId": "{{ToolPage}}", "variables": {"first": 51}}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"
jsonpath "$.errors[0].extensions.argument" == "first"

# 5. Search returns a union: tools, then categories.
POST {{api}}
{"documentId": "{{Search}}", "variables": {"text": "saw"}}
HTTP 200
[Asserts]
jsonpath "$.data.search[0].__typename" == "Tool"
jsonpath "$.data.search[*].__typename" contains "Category"

# 6. @oneOf: a tool by barcode, its specs by kind; a tool out on loan is not available.
POST {{api}}
{"documentId": "{{ToolDetail}}", "variables": {"by": {"barcode": "TS-0005"}}}
HTTP 200
[Asserts]
jsonpath "$.data.tool.name" == "Circular saw"
jsonpath "$.data.tool.specs.volts" == 230
jsonpath "$.data.tool.available" == false

# 7. @oneOf with both fields is refused by validation.
POST {{api}}
{"documentId": "{{ToolDetail}}", "variables": {"by": {"id": "t01", "barcode": "TS-0001"}}}
HTTP *
[Asserts]
jsonpath "$.errors" count >= 1
jsonpath "$.data.tool" not exists

# 8. Filters: categories with counts (DataLoader) and branches.
POST {{api}}
{"documentId": "{{Filters}}"}
HTTP 200
[Asserts]
jsonpath "$.data.categories" count == 6
jsonpath "$.data.categories[?(@.id == 'cat-saw')].toolCount" == 6
jsonpath "$.data.branches" count == 3

# 9. Signed out, "me" is null, not an error.
POST {{api}}
{"documentId": "{{MyLoans}}"}
HTTP 200
[Asserts]
jsonpath "$.data.me" == null
jsonpath "$.errors" not exists

# 10. Signed in: my loans, with tool names from the other subgraph.
POST {{api}}
Authorization: Bearer {{token_member}}
{"documentId": "{{MyLoans}}"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.me.displayName" == "Ada Okafor"
jsonpath "$.data.me.loans.edges[0].node.status" == "OPEN"
jsonpath "$.data.me.loans.edges[0].node.tool.name" == "Circular saw"
jsonpath "$.data.me.loans.edges[1].node.status" == "RETURNED"

# 11. Borrowing an out tool is a typed result, not an error.
POST {{api}}
Authorization: Bearer {{token_other}}
{"documentId": "{{BorrowTool}}", "variables": {"input": {"toolId": "t05", "idempotencyKey": "hurl-11-{{run}}"}}}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.borrowTool.__typename" == "ToolUnavailable"
jsonpath "$.data.borrowTool.message" contains "out until"
jsonpath "$.data.borrowTool.expectedBack" exists

# 12. Invalid input names the field.
POST {{api}}
Authorization: Bearer {{token_other}}
{"documentId": "{{BorrowTool}}", "variables": {"input": {"toolId": "t04", "days": 30, "idempotencyKey": "hurl-12-{{run}}"}}}
HTTP 200
[Asserts]
jsonpath "$.data.borrowTool.__typename" == "InvalidInput"
jsonpath "$.data.borrowTool.field" == "days"

# 13. Borrow, then the same request again: one loan, the same answer.
POST {{api}}
Authorization: Bearer {{token_other}}
{"documentId": "{{BorrowTool}}", "variables": {"input": {"toolId": "t04", "days": 3, "idempotencyKey": "hurl-13-{{run}}"}}}
HTTP 200
[Asserts]
jsonpath "$.data.borrowTool.__typename" == "Borrowed"
jsonpath "$.data.borrowTool.loan.tool.available" == false
[Captures]
loan_id: jsonpath "$.data.borrowTool.loan.id"

POST {{api}}
Authorization: Bearer {{token_other}}
{"documentId": "{{BorrowTool}}", "variables": {"input": {"toolId": "t04", "days": 3, "idempotencyKey": "hurl-13-{{run}}"}}}
HTTP 200
[Asserts]
jsonpath "$.data.borrowTool.__typename" == "Borrowed"
jsonpath "$.data.borrowTool.loan.id" == "{{loan_id}}"

# 14. Someone else's loan does not exist for you.
POST {{api}}
Authorization: Bearer {{token_member}}
{"documentId": "{{ReturnTool}}", "variables": {"loanId": "{{loan_id}}"}}
HTTP 200
[Asserts]
jsonpath "$.data.returnTool.__typename" == "LoanNotFound"

# 15. The borrower returns it; a second return is AlreadyReturned.
POST {{api}}
Authorization: Bearer {{token_other}}
{"documentId": "{{ReturnTool}}", "variables": {"loanId": "{{loan_id}}"}}
HTTP 200
[Asserts]
jsonpath "$.data.returnTool.__typename" == "Returned"
jsonpath "$.data.returnTool.loan.tool.available" == true

POST {{api}}
Authorization: Bearer {{token_other}}
{"documentId": "{{ReturnTool}}", "variables": {"loanId": "{{loan_id}}"}}
HTTP 200
[Asserts]
jsonpath "$.data.returnTool.__typename" == "AlreadyReturned"

# 16. Field-level authorization: a member sees that a tool is out, not who has it.
POST {{api}}
Authorization: Bearer {{token_other}}
{"documentId": "{{DeskLookup}}", "variables": {"barcode": "TS-0005"}}
HTTP 200
[Asserts]
jsonpath "$.data.tool.available" == false
jsonpath "$.data.tool.currentLoan.status" == "OPEN"
jsonpath "$.data.tool.currentLoan.member" == null
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"
jsonpath "$.errors[0].path" contains "member"

# 17. The librarian sees who has it.
POST {{api}}
Authorization: Bearer {{token_admin}}
{"documentId": "{{DeskLookup}}", "variables": {"barcode": "TS-0005"}}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.tool.currentLoan.member.displayName" == "Ada Okafor"

# 18. Librarians only: the overdue report.
POST {{api}}
Authorization: Bearer {{token_member}}
{"documentId": "{{OverdueReport}}"}
HTTP 200
[Asserts]
jsonpath "$.data" == null
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"

POST {{api}}
Authorization: Bearer {{token_admin}}
{"documentId": "{{OverdueReport}}"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.overdueLoans.edges[0].node.tool.name" == "Step ladder"
jsonpath "$.data.overdueLoans.edges[0].node.member.displayName" == "Olu Bankole"

# 19. Trusted documents: a query that is not in the manifest is refused before any subgraph sees it.
POST {{api}}
{"query": "{ tools(first: 1) { totalCount } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.code" == "PERSISTED_DOCUMENT_ID_REQUIRED"
jsonpath "$.data" not exists

# 20. Demand control: the overdue report at 50 x 50 costs too much, even for a librarian.
POST {{api}}
Authorization: Bearer {{token_admin}}
{"documentId": "{{OverdueReport}}", "variables": {"first": 50, "history": 50}}
HTTP 200
[Asserts]
header "x-cost-estimated" toInt > 2000
jsonpath "$.errors[0].extensions.code" == "COST_ESTIMATED_TOO_EXPENSIVE"

# 21. A token that is expired, or signed with another key, is refused.
POST {{api}}
Authorization: Bearer {{token_expired}}
{"documentId": "{{MyLoans}}"}
HTTP 200
[Asserts]
jsonpath "$.data.me" == null
jsonpath "$.errors[0].extensions.classification" == "UNAUTHORIZED"

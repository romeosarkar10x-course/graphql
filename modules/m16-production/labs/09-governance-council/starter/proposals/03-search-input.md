# Proposal 3: replace Query.search with searchBooks(input:) (platform team)

**Change.** Remove `Query.search(text: String!): [Book!]!` now; add
`Query.searchBooks(input: SearchInput!): BookSearchResult!` with filters (author, year range) and pagination.

**Why.** Search needs filters and pages; `search` returns every match at once.

**Usage today (7 days).** `Query.search` is used by web 3.2.0 (`Search`, about 10% of web's 853 operations).
No other client. Web deploys daily.

**Constraint.** The platform team wants to delete the old resolver this sprint to avoid maintaining two.

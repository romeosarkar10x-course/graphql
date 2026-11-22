# Module 18.4: the generated API over Module 9's club dataset (scale 1), schema m18_04. Variable: url.

# G01 read-only: no mutation type at all, and the loader's pick() helper is not a Query field
POST {{url}}
```graphql
{ __schema { mutationType { name } } __type(name: "Query") { fields { name } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.__schema.mutationType" == null
jsonpath "$.data.__type.fields[*].name" contains "allClubs"
jsonpath "$.data.__type.fields[*].name" not contains "pick"

# G02 one club, its owner, its current book and the first members by join date, through foreign keys
POST {{url}}
```graphql
{ clubByRowId(rowId: "1") { name readerByOwnerId { handle name } bookByCurrentBookId { title author }
    membershipsByClubId(first: 3, orderBy: JOINED_AT_ASC) { totalCount nodes { readerByReaderId { handle } } } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.clubByRowId.name" == "Last Mirror Club"
jsonpath "$.data.clubByRowId.readerByOwnerId.handle" == "reader8"
jsonpath "$.data.clubByRowId.bookByCurrentBookId.title" == "The Glass Station"
jsonpath "$.data.clubByRowId.membershipsByClubId.totalCount" == 25
jsonpath "$.data.clubByRowId.membershipsByClubId.nodes[0].readerByReaderId.handle" == "reader54"
jsonpath "$.data.clubByRowId.membershipsByClubId.nodes[1].readerByReaderId.handle" == "reader133"

# G03 a club with no current book: null, not an error
POST {{url}}
```graphql
{ clubByRowId(rowId: "10") { name bookByCurrentBookId { title } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.clubByRowId.name" == "Distant Letter Club"
jsonpath "$.data.clubByRowId.bookByCurrentBookId" == null

# G04 counts and filters on indexed columns
POST {{url}}
```graphql
{ allBooks { totalCount } allReviews(condition: { bookId: "1" }) { totalCount }
  readerByRowId(rowId: "1") { handle membershipsByReaderId { totalCount } reviewsByAuthorId { totalCount } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.allBooks.totalCount" == 2000
jsonpath "$.data.allReviews.totalCount" == 9
jsonpath "$.data.readerByRowId.handle" == "reader1"
jsonpath "$.data.readerByRowId.membershipsByReaderId.totalCount" == 1
jsonpath "$.data.readerByRowId.reviewsByAuthorId.totalCount" == 10

# G05 cursor pagination: page two starts where page one ended
POST {{url}}
```graphql
query($after: Cursor) { allClubs(first: 2, after: $after, orderBy: ROW_ID_ASC) { nodes { rowId } pageInfo { endCursor hasNextPage } } }
```
HTTP 200
[Captures]
cursor: jsonpath "$.data.allClubs.pageInfo.endCursor"
[Asserts]
jsonpath "$.data.allClubs.nodes[1].rowId" == "2"
jsonpath "$.data.allClubs.pageInfo.hasNextPage" == true
POST {{url}}
```graphql
query($after: Cursor) { allClubs(first: 2, after: $after, orderBy: ROW_ID_ASC) { nodes { rowId name } } }
variables {
  "after": "{{cursor}}"
}
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.allClubs.nodes[0].rowId" == "3"
jsonpath "$.data.allClubs.nodes[1].rowId" == "4"

# G06 global object identification: any node by its opaque id
POST {{url}}
```graphql
{ allBooks(first: 1, orderBy: ROW_ID_ASC) { nodes { id } } }
```
HTTP 200
[Captures]
book: jsonpath "$.data.allBooks.nodes[0].id"
POST {{url}}
```graphql
query($id: ID!) { node(id: $id) { __typename ... on Book { rowId title } } }
variables {
  "id": "{{book}}"
}
```
HTTP 200
[Asserts]
jsonpath "$.data.node.__typename" == "Book"
jsonpath "$.data.node.title" == "The Iron Forest"

# G07 a write is refused and changes nothing
POST {{url}}
```graphql
mutation { deleteReviewByRowId(input: { rowId: "1" }) { deletedReviewId } }
```
HTTP *
[Asserts]
jsonpath "$.errors" count >= 1
POST {{url}}
```graphql
{ reviewByRowId(rowId: "1") { rowId } }
```
HTTP 200
[Asserts]
jsonpath "$.data.reviewByRowId.rowId" == "1"

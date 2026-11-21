# Lab 6.5 notes

## SQL emitted (`spring.jpa.show-sql: true`)

From `_workbench/2026-11-21-m05-probe/spring-data-sql.txt`:

| Query | SQL |
|---|---|
| `book(id: "b2") { title }` | `select b1_0.id,b1_0.author,b1_0.isbn,b1_0.published_year,b1_0.title from book b1_0 where b1_0.id=? fetch first ? rows only` (query by example, not `findById`) |
| `books(author: ..., publishedYear: 2021) { id }` | `... from book b1_0 where b1_0.published_year=? and b1_0.author=?` |
| `searchBooks(titleContains: "THE") { id }` | `... where upper(b1_0.title) like upper(?) escape '\' order by b1_0.published_year` |

## Questions

1. All five columns, every time: both fetchers load whole entities, and the GraphQL selection never
   reaches the repository. Doing better means reading `DataFetchingFieldSelectionSet` (5.2) and building
   a projection or a custom query; Spring for GraphQL can pass the selection to Querydsl or Query by Example
   only as an entity example, not as a column list.
2. Anything that is not exact equality on entity properties: title contains, year ranges (`publishedAfter`),
   sorting, pagination limits, OR conditions ("by Le Guin or Ishiguro"), or anything joining another table
   (books with an average rating above 4). The hand-written `searchBooks` exists because QBE cannot say "contains".
3. The GraphQL contract silently follows the entity: rename `author` to `authorName` in Java and the
   generated argument binding stops matching, so `books(author:)` quietly matches nothing or the
   inspection flags `Book.author` as unmapped. Clients see breakage they did not cause; the database
   team did not know they owned an API.
4. Only for internal or admin read-only lookups where the schema can mirror a table (`book(id:)`), and
   with schema inspection failing startup (6.3). Not for Shelfie's public schema: its fields are product
   concepts (ratings, members, viewer), which do not map 1:1 to tables, and I want the contract decoupled
   from the database.

# Lab 6.5 notes

## SQL emitted (`spring.jpa.show-sql: true`)

| Query | SQL |
|---|---|
| `book(id: "b2") { title }` | |
| `books(author: ..., publishedYear: 2021) { id }` | |
| `searchBooks(titleContains: "THE") { id }` | |

## Questions

1. The client asked for `title` only. Which columns did the SQL read, and why can neither kind of fetcher
   do better without extra work?
2. Name two queries a client might reasonably want that the generated `books` fetcher cannot express.
3. Generated fetchers put your database schema one annotation away from your GraphQL schema. What
   breaks, and for whom, when someone renames a column or entity property?
4. Would you use `@GraphQlRepository` for Shelfie? For which fields, if any?
